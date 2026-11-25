package com.ourgalaxy.memory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MemoryTagMapper extends BaseMapper<MemoryTag> {

    @Select("SELECT * FROM memory_tag WHERE couple_id = #{coupleId} AND memory_id = #{memoryId} ORDER BY id")
    List<MemoryTag> selectHistory(Long coupleId, Long memoryId);

    @Update("UPDATE memory_tag SET deleted = 0, name = #{name}, update_time = UTC_TIMESTAMP(6) "
            + "WHERE id = #{id} AND couple_id = #{coupleId} AND memory_id = #{memoryId}")
    int restore(Long id, Long coupleId, Long memoryId, String name);
}
