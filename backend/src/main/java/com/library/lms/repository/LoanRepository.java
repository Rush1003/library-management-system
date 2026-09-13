package com.library.lms.repository;

import com.library.lms.model.Loan;
import com.library.lms.model.LoanStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {

    List<Loan> findByMemberId(String memberId);

    List<Loan> findByBookId(String bookId);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByMemberIdAndStatus(String memberId, LoanStatus status);
}
