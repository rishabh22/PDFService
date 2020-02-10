package nic.oad.pdfservice.dao;

import nic.oad.pdfservice.model.domain.EmpMast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpMastRepo extends JpaRepository<EmpMast,Long> {
    public EmpMast findByEmpCode(Long empCode);
}
