package com.ourgalaxy.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoupleSpaceMapper extends BaseMapper<CoupleSpace> {

    @Select("SELECT * FROM couple_space WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    CoupleSpace selectByIdForUpdate(Long id);
}
