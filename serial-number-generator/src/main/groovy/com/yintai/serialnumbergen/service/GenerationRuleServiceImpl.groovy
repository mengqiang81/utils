package com.yintai.serialnumbergen.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.yintai.serialnumbergen.domain.GenerationRule
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
/**
 * Created by hanguozhu on 2015/8/16.
 */
@Service
@Transactional
@CompileStatic
public class GenerationRuleServiceImpl implements GenerationRuleService {

    @Autowired
    private GenerationRuleRepos generationRuleRepos
    @Autowired
    private ObjectMapper objectMapper
    @Autowired
    private HiloAlgorithmServiceImpl hiloAlgorithmService
    @Autowired
    private CommonAlgorithmServiceImpl commonAlgorithmService

    @Override
    public GenerationRuleDTO findGenerationRule(Long id) {
        def rule = generationRuleRepos.findOne(id);
        if (rule) {
            def algorithmParams
            switch (rule.algorithm) {
                case AlgorithmEnum.HILO:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, HiloAlgorithmDTO)
                    break
                case AlgorithmEnum.COMMON:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, CommonAlgorithmDTO)
                    break
                default:
                    algorithmParams = null
            }
            return new GenerationRuleDTO(algorithm: rule.algorithm, ruleCode: rule.ruleCode, algorithmParams: algorithmParams, description: rule.description)
        }
        null
    }

    @Override
    public GenerationRuleDTO findByRuleCode(String ruleCode) {
        def rule = generationRuleRepos.findByRuleCode(ruleCode)
        if (rule) {
            def algorithmParams
            switch (rule.algorithm) {
                case AlgorithmEnum.HILO:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, HiloAlgorithmDTO)
                    break
                case AlgorithmEnum.COMMON:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, CommonAlgorithmDTO)
                    break
                default:
                    algorithmParams = null
            }
            return new GenerationRuleDTO(algorithm: rule.algorithm, ruleCode: rule.ruleCode, algorithmParams: algorithmParams, description: rule.description)
        }
        null
    }

    @Override
    public GenerationRuleDTO findByRuleCodeAndAlgorithm(String ruleCode, String algorithm) {
        def rule = generationRuleRepos.findByRuleCodeAndAlgorithm(ruleCode, algorithm)
        if (rule) {
            def algorithmParams
            switch (rule.algorithm) {
                case AlgorithmEnum.HILO:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, HiloAlgorithmDTO)
                    break
                case AlgorithmEnum.COMMON:
                    algorithmParams = objectMapper.readValue(rule.algorithmParams, CommonAlgorithmDTO)
                    break
                default:
                    algorithmParams = null
            }
            return new GenerationRuleDTO(algorithm: rule.algorithm, ruleCode: rule.ruleCode, algorithmParams: algorithmParams, description: rule.description)
        }
        null
    }

    @Override
    String create(GenerationRuleDTO generationRuleDTO) {
        switch (generationRuleDTO.algorithm) {
            case AlgorithmEnum.HILO:
                hiloAlgorithmService.init(generationRuleDTO.algorithmParams as HiloAlgorithmDTO)
                break
            case AlgorithmEnum.COMMON:
                commonAlgorithmService.init(generationRuleDTO.algorithmParams as CommonAlgorithmDTO)
                break
            default:
                throw new NotSupportedException("算法不支持")
        }

        def generationRule = generationRuleRepos.save(
                new GenerationRule(
                        algorithm: generationRuleDTO.algorithm,
                        ruleCode: generationRuleDTO.ruleCode,
                        algorithmParams: objectMapper.writeValueAsString(generationRuleDTO.algorithmParams),
                        description: generationRuleDTO.description
                )
        )
        return generationRule.id
    }
}
