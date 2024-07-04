package com.yintai.serialnumbergen.service

import com.yintai.serialnumbergen.domain.SerialNumNextHi
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

/**
 * Created by hanguozhu on 2015/8/16.
 */
public interface SeriaNumNextHiRepos extends CrudRepository<SerialNumNextHi,Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM SerialNumNextHi c WHERE c.channel = :channel")
    void deleteByChannel(@Param("channel")String channel);
}
