package com.yintai.serialnumbergen.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
/**
 * Created by hanguozhu on 2015/9/16.
 */
@Entity
class SerialNumNextHi implements Serializable{
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    Long nextHi
    @Column(nullable = false,unique = true)
    String channel
}
