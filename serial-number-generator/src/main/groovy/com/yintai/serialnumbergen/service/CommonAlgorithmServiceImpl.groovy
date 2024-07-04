package com.yintai.serialnumbergen.service

import com.yintai.serialnumbergen.domain.SerialNumNextHi
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Qiang on 27/04/2017.
 */
@Service
class CommonAlgorithmServiceImpl implements AlgorithmService<CommonAlgorithmDTO> {

    @Autowired
    HiloAlgorithmServiceImpl hiloAlgorithmService

    @Autowired
    SeriaNumNextHiRepos seriaNumNextHiRepos

    String gen(CommonAlgorithmDTO commonAlgorithmDTO) {
        def hiloStr = hiloAlgorithmService.gen(new HiloAlgorithmDTO(unionCode: commonAlgorithmDTO.unionCode, numberLength: commonAlgorithmDTO.numberLength, start: commonAlgorithmDTO.start, maxLo: commonAlgorithmDTO.maxLo))
        def dateStr
        if(commonAlgorithmDTO.dateFormat && commonAlgorithmDTO.zone){
            dateStr = new DateTime(DateTimeZone.forID(commonAlgorithmDTO.zone)).toString(commonAlgorithmDTO.dateFormat)
        }else if(commonAlgorithmDTO.dateFormat){
            dateStr = new DateTime(DateTimeZone.UTC).toString(commonAlgorithmDTO.dateFormat)
        }else if(commonAlgorithmDTO.zone){
            dateStr = new DateTime(DateTimeZone.forID(commonAlgorithmDTO.zone)).toString('yyyyMMdd')
        }else {
            dateStr = new DateTime(DateTimeZone.UTC).toString('yyyyMMdd')
        }
       return  "${commonAlgorithmDTO.prefix?:''}${dateStr}${hiloStr}${commonAlgorithmDTO.postfix?:''}"
    }

    @Override
    void init(CommonAlgorithmDTO algorithmParams) {
        seriaNumNextHiRepos.save(new SerialNumNextHi(nextHi: 0L, channel: algorithmParams.unionCode))
    }
}
