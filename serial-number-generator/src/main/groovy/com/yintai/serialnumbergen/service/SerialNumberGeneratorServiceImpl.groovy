package com.yintai.serialnumbergen.service

import groovy.transform.CompileStatic
import org.nofdev.logging.CustomLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 序列号生成入口
 * Created by hanguozhu on 2015/9/16.
 */

@Service
@Transactional
@CompileStatic
class SerialNumberGeneratorServiceImpl implements SerialNumberGeneratorService {

    private static final CustomLogger log = CustomLogger.getLogger(SerialNumberGeneratorServiceImpl.class);

    @Autowired
    private GenerationRuleService generationRuleService

    @Autowired
    private HiloAlgorithmServiceImpl hiloAlgorithmService
    @Autowired
    private CommonAlgorithmServiceImpl commonAlgorithmService

    @Override
    String genNumber(String ruleCode) {
        GenerationRuleDTO generationRuleDTO = generationRuleService.findByRuleCode(ruleCode)
        switch (generationRuleDTO.algorithm) {
            case AlgorithmEnum.HILO:
                def nextHiDTO = generationRuleDTO.algorithmParams as HiloAlgorithmDTO
                return this.hiloAlgorithmService.gen(nextHiDTO)
            case AlgorithmEnum.COMMON:
                def commonAlgorithmDTO = generationRuleDTO.algorithmParams as CommonAlgorithmDTO
                return this.commonAlgorithmService.gen(commonAlgorithmDTO)
            default:
                throw new NotSupportedException('算法不被支持')
        }
    }

}