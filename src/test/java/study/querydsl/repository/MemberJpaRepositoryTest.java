package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest  {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;


    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> findAllResult = memberJpaRepository.findAll_QueryDSL();
        assertThat(findAllResult).containsExactly(member);

        List<Member> findByUsernameResult = memberJpaRepository.findByUsername_QueryDSL("member1");
        assertThat(findByUsernameResult).containsExactly(member);

    }
}