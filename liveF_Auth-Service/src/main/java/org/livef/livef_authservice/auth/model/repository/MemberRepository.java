package org.livef.livef_authservice.auth.model.repository;

import java.util.Optional;

import org.livef.livef_authservice.auth.model.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity,Long>{

	Optional<MemberEntity> findByMemberId(String memberId);
}
