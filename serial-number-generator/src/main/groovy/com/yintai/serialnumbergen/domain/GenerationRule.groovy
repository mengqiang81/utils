package com.yintai.serialnumbergen.domain

import com.yintai.serialnumbergen.service.AlgorithmEnum

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id
/**
 * Created by hanguozhu on 2015/8/16.
 */
@Entity
//@TypeDefs([
//        @TypeDef(name = "json", typeClass = JsonStringType.class)
//])
public class GenerationRule {
    @Id
    @GeneratedValue
    Long id

    @Enumerated(EnumType.STRING)
    AlgorithmEnum algorithm //算法 目前只支持 HILO 和 COMMON

    @Column(unique = true)
    String ruleCode //规则编码

    //泛型只适合能够枚举的场景, 微格式叫法不合适, 应该叫用户自定义参数
//    @Type(type = 'json')
    String algorithmParams

    String description

//    //以下为特定算法需要的参数, 如初始值, 步长(默认为1), 缓存值(默认为20)等
//    @Column(nullable = true)
//    Integer maxLo
//    Integer numberLength//长度
}
