package com.ourgalaxy.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoupleInvitationMapper extends BaseMapper<CoupleInvitation> {

    @Select("SELECT * FROM couple_invitation WHERE code = #{code} AND deleted = 0 FOR UPDATE")
    CoupleInvitation selectByCodeForUpdate(String code);
}
