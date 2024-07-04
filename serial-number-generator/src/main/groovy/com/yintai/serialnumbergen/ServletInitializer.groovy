package com.yintai.serialnumbergen

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.support.SpringBootServletInitializer
/**
 * Created by Qiang on 01/04/2017.
 */
class ServletInitializer extends SpringBootServletInitializer{
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {

        application.sources(Application)
    }
}
