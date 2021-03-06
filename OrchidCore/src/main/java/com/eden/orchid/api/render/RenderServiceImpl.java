package com.eden.orchid.api.render;

import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.options.annotations.Archetype;
import com.eden.orchid.api.options.annotations.BooleanDefault;
import com.eden.orchid.api.options.annotations.Description;
import com.eden.orchid.api.options.annotations.Option;
import com.eden.orchid.api.options.archetypes.ConfigArchetype;
import com.eden.orchid.api.resources.resource.InlineResource;
import com.eden.orchid.api.theme.assets.AssetPage;
import com.eden.orchid.api.theme.pages.OrchidPage;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * @since v1.0.0
 * @orchidApi services
 */
@Description(value = "How Orchid renders pages and writes them to disk.", name = "Render")
@Archetype(value = ConfigArchetype.class, key = "services.renderer")
public class RenderServiceImpl implements RenderService {
    protected OrchidContext context;
    protected OrchidRenderer renderer;
    @Option
    @BooleanDefault(false)
    @Description("On a dry run, pages are indexed but not rendered.")
    public boolean dry;
    @Option
    @BooleanDefault(false)
    @Description("Normally, draft pages are not rendered along with the rest of the site, but this behavior can be turned off by setting this value to `true`.")
    public boolean includeDrafts;

    @Inject
    public RenderServiceImpl(OrchidContext context, OrchidRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
    }

    @Override
    public void initialize(OrchidContext context) {
        this.context = context;
    }

    private InputStream getRenderedTemplate(OrchidPage page) {
        page.setCurrent(true);
        String content = "" + page.renderInLayout();
        page.setCurrent(false);
        return toStream(content);
    }

    private boolean renderTemplate(final OrchidPage page) {
        return renderInternal(page, () -> getRenderedTemplate(page));
    }

    private InputStream getRenderedRaw(OrchidPage page) {
        page.setCurrent(true);
        String content = page.getResource().getContent();
        if (page.getResource().shouldPrecompile()) {
            content = context.compile(page.getResource(), page.getResource().getPrecompilerExtension(), content, page);
        }
        content = "" + context.compile(page.getResource(), page.getResource().getReference().getExtension(), content, page);
        page.setCurrent(false);
        return toStream(content);
    }

    private boolean renderRaw(final OrchidPage page) {
        return renderInternal(page, () -> getRenderedRaw(page));
    }

    private InputStream getRenderedBinary(OrchidPage page) {
        page.setCurrent(true);
        InputStream is = page.getResource().getContentStream();
        page.setCurrent(false);
        return is;
    }

    private boolean renderBinary(final OrchidPage page) {
        return renderInternal(page, () -> getRenderedBinary(page));
    }

    @Override
    public boolean render(OrchidPage page) {
        RenderMode renderMode = page.getPageRenderMode();
        switch (renderMode) {
        case TEMPLATE: 
            return renderTemplate(page);

        case RAW: 
            return renderRaw(page);

        case BINARY: 
            return renderBinary(page);

        default: 
            throw new IllegalArgumentException("Dynamic RenderMode rendering must be one of [TEMPLATE, RAW, BINARY]");
        }
    }

    @Override
    public InputStream getRendered(OrchidPage page) {
        RenderMode renderMode = page.getPageRenderMode();
        switch (renderMode) {
        case TEMPLATE: 
            return getRenderedTemplate(page);

        case RAW: 
            return getRenderedRaw(page);

        case BINARY: 
            return getRenderedBinary(page);

        default: 
            throw new IllegalArgumentException("Dynamic RenderMode rendering must be one of [TEMPLATE, RAW, BINARY]");
        }
    }

    InputStream toStream(String content) {
        try {
            return IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    boolean skipPage(OrchidPage page) {
        return dry || (page.isDraft() && !includeDrafts) || !page.shouldRender();
    }

    boolean renderInternal(OrchidPage page, Supplier<InputStream> factory) {
        long startTime = System.currentTimeMillis();
        long stopTime;
        boolean result = false;
        if (!skipPage(page)) {
            result = renderer.render(page, factory.get());
        }
        stopTime = System.currentTimeMillis();
        context.onPageGenerated(page, stopTime - startTime);
        return result;
    }

    @Override
    public boolean renderAsset(AssetPage asset) {
        boolean success = false;
        if (!(asset.getResource() instanceof InlineResource)) {
            success = render(asset);
        }
        asset.setRendered(true);
        return success;
    }

    public boolean includeDrafts() {
        return this.includeDrafts;
    }
}
