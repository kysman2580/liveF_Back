package com.livef.livef_memberservice.member.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Entity
@Table(name = "member")
@Builder
public class MemberEntity {
	 @Id @Column(name = "memberNo") @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long memberNo;

     @Column(name = "memberId", nullable = false, length = 30, unique = true)
     private String memberId;

     @Column(name = "memberPw", nullable = false, length = 100)
     private String memberPw;
     
     @Column(name = "memberName", nullable = false, length = 20)
     private String memberName;

     @Column(name = "memberNickname", nullable = false, length = 20, unique = true)
     private String memberNickname;
     
     @Column(name = "memberPhone", nullable = false, length = 20)
     private String memberPhone;
     
     @Column(name = "enrollDate", nullable = false, updatable = false)
     @org.hibernate.annotations.CreationTimestamp
     private LocalDateTime enrollDate;
     
     @Column(name = "isActive", nullable = false)
     @Builder.Default
     private String isActive = "Y";

     @Column(name = "memberRole", nullable = false, length = 20)
     @Builder.Default
     private String memberRole = "user";
     
     public void updateAll(String memberName,
			               String memberNickname,
			               String memberPhone,
			               String memberRole,
			               String isActive,         // "Y" 또는 "N"
			               String encodedPassword   // 이미 인코딩된 값
						  ) {
			this.memberName = memberName;
			this.memberNickname = memberNickname;
			this.memberPhone = memberPhone;
			this.memberRole = memberRole;
			this.isActive = isActive;
			if (encodedPassword != null) {
			this.memberPw = encodedPassword;
			}

     }
} 
