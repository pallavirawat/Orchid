package com.eden.orchid.netlifycms.pages

import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.server.OrchidView
import com.eden.orchid.netlifycms.api.NetlifyCmsManageController
import com.eden.orchid.netlifycms.model.NetlifyCmsModel

@Description(value = "The page hosting the Netlify CMS.", name = "Netlify CMS View")
class NetlifyCmsAdminView(
        context: OrchidContext,
        controller: NetlifyCmsManageController,
        val model: NetlifyCmsModel
) : OrchidView(context, controller, "manage") {

    init {
        this.title = "Orchid Content Manager"
        this.type = Type.Fullscreen
    }

}
