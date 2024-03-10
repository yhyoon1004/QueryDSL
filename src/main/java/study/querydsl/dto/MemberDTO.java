package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDTO {
    private String username;
    private int age;

    @QueryProjection    //해당 어노테이션을 사용하면 DTO도 Q클래스 파일로 생성함
    public MemberDTO(String username, int age) {
        this.username = username;
        this.age = age;
    }


}
