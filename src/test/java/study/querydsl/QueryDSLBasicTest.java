package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDTO;
import study.querydsl.dto.QMemberDTO;
import study.querydsl.dto.UserDTO;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;// static import 로 해당 인스턴스를 가져다쓰는 방법
import static study.querydsl.entity.QTeam.team;

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
    public void startJPQL() throws Exception {
        Member member = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(member.getUsername()).isEqualTo("member1");
    }


    @Test
    public void startQueryDSL() throws Exception {
//        QMember memberAAA = new QMember("memberAAA"); //생성자로 생성할 경우 엘리어스를 생성자의 문자열로 주게됨
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void search() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)//이 메서드는 'select(member).from(member)'메서드를 합친 것
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),  //쉼표로 and() 메서드 대체 가능
                        member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() throws Exception {
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
    public void sortTest() throws Exception {
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

    @Test
    public void pagingTest() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void pagingTotalTest() throws Exception {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * */
    @Test
    public void groupByTest () throws Exception{
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                ).from(member).join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void joinTest () throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    /** 세타 조인
     * 회원의 이름이 팀의 이름과 같은 회원 조회
     * */
    @Test
    public void thetaJoinTest () throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //연관관계 없이 조인 -> but 이 방법은 외부 조인 불ㄱ

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     *  예) 회원과 팀을 조인하며, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     *  JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     *  */
    @Test
    public void joinOnTest () throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        //select 한 값이 특정 객체가 아닌 여러가지 값이므로 tuple 타입을 반환

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * 연관관계가 없는 엔티티를 외부조인
     * 회원의 이름이 팀의 이름과 같은 대상을 외부 조인
     * */
    @Test
    public void joinOnNoRelationTest () throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        // on -> 연관관계가 없는 엔티티들을 조인할 경우 조인할 조건 값으로 처리할 때 사용

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void noFetchJoinTest () throws Exception{
        em.flush();
        em.clear();

        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
                            //영속성 컨텍스트에 해당 객체를 가져왔는 지 확인하는 메서드
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(isLoaded).as("페치 조인 미적용").isFalse();
    }
    @Test
    public void fetchJoinTest () throws Exception{
        em.flush();
        em.clear();
        //.fetchJoin() 매서드를 걸어주면 연관관계의 엔티티를 Fetch Join 해인ㅂ
        Member member1 = queryFactory
                .selectFrom(member)
                .join(member.team, team ).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
                            //영속성 컨텍스트에 해당 객체를 가져왔는 지 확인하는 메서드
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(isLoaded).as("페치 조인  적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * */
    @Test
    public void subQueryTest () throws Exception{
        QMember memberSubQ = new QMember("memberSubQ");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSubQ.age.max())
                                .from(memberSubQ)
                )).fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryTest2 () throws Exception{
        QMember memberSubQ = new QMember("memberSubQ");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSubQ.age.avg())
                                .from(memberSubQ)
                )).fetch();
        assertThat(result)
                .extracting("age")
                .containsExactly(30,40);
    }
    /**
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryTest3 () throws Exception{
        QMember memberSubQ = new QMember("memberSubQ");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSubQ.age)
                                .from(memberSubQ)
                                .where(memberSubQ.age.gt(10))
                )).fetch();
        assertThat(result)
                .extracting("age")
                .containsExactly(20 ,30,40);
    }
    @Test
    public void subQueryTest4 () throws Exception{
        QMember memberSubQ = new QMember("memberSubQ");
        List<Tuple> result = queryFactory
                .select(member.username, JPAExpressions.select(memberSubQ.age.avg()).from(memberSubQ))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /**
         * JPA 서브 쿼리는 from 절의 서브쿼리(인라인 뷰)를 사용하지 못함 -> QueryDSL 도 마찬가지로 지원하지 않음
         * __해결방안__
         * 1. 서브쿼리를 join으로 해결할 수 있으면 그렇게 처리
         * 2. 쿼리를 2번 실행하여 처리
         * 3. nativeSQL 실행
         * */
    }

    @Test
    public void caseTest () throws Exception{
        List<String> result = queryFactory
                .select(
                        member.age.when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타")
                ).from(member)
                .fetch();

        for (String item : result) {
            System.out.println("item = " + item);
        }
    }

    @Test
    public void complexCaseTest () throws Exception{
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20 살")
                                .when(member.age.between(21, 30)).then("21~30 살")
                                .otherwise("기타")
                ).from(member)
                .fetch();

        for (String item : result) {
            System.out.println("item = " + item);
        }
    }

    
    @Test
    public void constantTest () throws Exception{
        List<Tuple> result = queryFactory
                .select(
                member.username, Expressions.constant("A")// Expressions를 이용해 상수 넣는 법
                )
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concatTest () throws Exception{
        List<String> result = queryFactory
                .select(
                        member.username.concat("_").concat(member.age.stringValue())// 값이 정수 타입이므로 문자열 변환
                )
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String item : result) {
            System.out.println("item = " + item);
        }
    }
    
    @Test
    public void tupleProjectionTest () throws Exception{
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDTOByJPQL() throws Exception {
        List<MemberDTO> resultList = em.createQuery(
                "select new study.querydsl.dto.MemberDTO(m.username, m.age) from Member m", MemberDTO.class
        ).getResultList();

        for (MemberDTO memberDTO : resultList) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOByQueryDSL () throws Exception{
        // setter 이용해서 값이 주입됨
        List<MemberDTO> result = queryFactory
                .select(Projections.bean(MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOByFieldOfQueryDSL () throws Exception{
        //getter setter 없어도 됨,
        List<MemberDTO> result = queryFactory
                .select(Projections.fields(MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }
    @Test
    public void findDTOByConstructorOfQueryDSL () throws Exception{
        // 매개변수로 들어가는 값의 타입이 맞아야함
        List<MemberDTO> result = queryFactory
                .select(Projections.constructor( MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDTOByFieldOfQueryDSL () throws Exception{
        QMember sQMember = new QMember("SubQueryMember");

        List<UserDTO> result = queryFactory
                .select(
                        Projections.fields(
                                //해당 DTO의 필드가 매칭되는 게 없어 null이 들어 오게 되는 데 이때 .as("DTO 필드 명")매서드를 사용하면 매칭하여 찾아줌
                                UserDTO.class, member.username.as("name"),
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(sQMember.age.max())
                                                .from(sQMember),
                                        "age"
                                )
                        )
                )
                .from(member)
                .fetch();

        for (UserDTO userDTO : result) {
            System.out.println("memberDTO = " + userDTO );
        }
    }


    @Test
    public void findDByQueryProjectionTest () throws Exception{
        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }
}
