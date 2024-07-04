package com.yintai.serialnumbergen

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.Profile

/**
 * Created by Qiang on 29/08/2017.
 */
@Profile(["pre"])
@Configuration
@ImportResource(locations=["classpath:hsf-provider-beans-pre.xml"])
class ResourcePreConfiguration {
}
