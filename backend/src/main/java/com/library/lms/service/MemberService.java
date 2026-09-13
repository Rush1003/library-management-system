package com.library.lms.service;

import com.library.lms.exception.DatabaseOperationException;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Member;
import com.library.lms.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers() {
        return safeCall(memberRepository::findAll, "fetch all members");
    }

    public Member getMemberById(String id) {
        return safeCall(() -> memberRepository.findById(id), "fetch member by id")
                .orElseThrow(() -> ResourceNotFoundException.forId("Member", id));
    }

    public Member createMember(Member member) {
        boolean exists = safeCall(() -> memberRepository.existsByEmail(member.getEmail()), "check email uniqueness");
        if (exists) {
            throw new DuplicateResourceException("A member with email " + member.getEmail() + " already exists");
        }
        member.setId(null);
        if (member.getMembershipDate() == null) {
            member.setMembershipDate(LocalDate.now());
        }
        return safeCall(() -> memberRepository.save(member), "create member");
    }

    public Member updateMember(String id, Member updated) {
        Member existing = getMemberById(id);
        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setActive(updated.isActive());
        if (!existing.getEmail().equals(updated.getEmail())) {
            boolean exists = safeCall(() -> memberRepository.existsByEmail(updated.getEmail()), "check email uniqueness");
            if (exists) {
                throw new DuplicateResourceException("A member with email " + updated.getEmail() + " already exists");
            }
            existing.setEmail(updated.getEmail());
        }
        return safeCall(() -> memberRepository.save(existing), "update member");
    }

    public void deleteMember(String id) {
        getMemberById(id);
        safeRun(() -> memberRepository.deleteById(id), "delete member");
    }

    private <T> T safeCall(DbCall<T> call, String action) {
        try {
            return call.execute();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Database error while trying to {}", action, ex);
            throw new DatabaseOperationException("Failed to " + action + " due to a database error", ex);
        }
    }

    private void safeRun(DbAction action, String description) {
        try {
            action.execute();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Database error while trying to {}", description, ex);
            throw new DatabaseOperationException("Failed to " + description + " due to a database error", ex);
        }
    }

    @FunctionalInterface
    private interface DbCall<T> {
        T execute();
    }

    @FunctionalInterface
    private interface DbAction {
        void execute();
    }
}
