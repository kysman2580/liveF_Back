package com.livef.livef_memberservice.member.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livef.livef_memberservice.member.model.entity.MemberEntity;

public interface MemberRepository extends JpaRepository<MemberEntity, Long>{

}
