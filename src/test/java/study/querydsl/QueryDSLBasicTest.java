package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;// static import 로 해당 인스턴스를 가져다쓰는 방법

@SpringBootTest
@Transactional
public class QueryDSLBasicTest {

    @PersistenceContext
    EntityManager em;
    JPAQueryFactory queryFactory;


    // 사전에 데이터를 찾아 넣음
    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        //스프링에서 해당 태스트 코드 실행 전에 주입해주어서 다른 메서드가 참조해서 사용해도 해당 클래드가 담겨있는 채로 수행가능

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    public void startJPQL () throws Exception{
        Member member = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(member.getUsername()).isEqualTo("member1");
    }
    
    
    @Test
    public void startQueryDSL () throws Exception{
//        QMember memberAAA = new QMember("memberAAA"); //생성자로 생성할 경우 엘리어스를 생성자의 문자열로 주게됨
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void search() throws Exception{
        Member findMember = queryFactory
                .selectFrom(member)//이 메서드는 'select(member).from(member)'메서드를 합친 것
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() throws Exception{
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),  //쉼표로 and() 메서드 대체 가능
                        member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest () throws Exception{
        //fetch() -> 결과가 없으면 빈 List 반환, fetchOne() -> 결과가 없으면 null 반환, 2개 이상이면 예외발생
//        List<Member> fetch =  queryFactory.selectFrom(member).fetch();
//
//        Member fetchOne = queryFactory.selectFrom(member).fetchOne();
//
//        Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();
//
//        QueryResults<Member> fetchResults = queryFactory.selectFrom(member).fetchResults();
//        fetchResults.getTotal();
//        List<Member> content = fetchResults.getResults();
        long fetchCount = queryFactory.selectFrom(member).fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 desc
     * 2. 회원 이름 올림차순 asc
     * 단 2 에서 회원 이름이 없으면 마지막에 출력  ->> nullsLast()
     */
    @Test
    public void sortTest () throws Exception{
        em.persist(new Member(null, 100));
        em.persist(new Member("member10", 100));
        em.persist(new Member("member11", 100));
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member10 = result.get(0);
        Member member11 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member10.getUsername()).isEqualTo("member10");
        assertThat(member11.getUsername()).isEqualTo("member11");
        assertThat(memberNull.getUsername()).isNull();

    }


}
