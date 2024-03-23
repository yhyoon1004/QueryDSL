package study.querydsl.repository;


import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;

import java.util.List;

public interface MemberCustomRepository {

    List<MemberTeamDTO> search(MemberSearchCondition condition);
}
