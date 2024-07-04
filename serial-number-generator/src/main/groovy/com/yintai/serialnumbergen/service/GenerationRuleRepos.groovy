package com.yintai.serialnumbergen.service

import com.yintai.serialnumbergen.domain.GenerationRule
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

/**
 * Created by hanguozhu on 2015/8/16.
 */
public interface GenerationRuleRepos extends CrudRepository<GenerationRule,Long> {

    GenerationRule findById(Long id);
    GenerationRule findByRuleCode(String ruleCode);


//    @Query("select u from GenerationRule u where u.ruleCode = :ruleCode and u.algorithm =:algorithm")
    GenerationRule findByRuleCodeAndAlgorithm(String ruleCode, String algorithm);

    @Transactional
    @Modifying
    @Query("DELETE FROM GenerationRule c WHERE c.ruleCode = :ruleCode")
    void deleteByRuleCode(@Param("ruleCode")String ruleCode);

}
