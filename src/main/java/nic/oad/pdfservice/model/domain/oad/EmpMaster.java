package nic.oad.pdfservice.model.domain.oad;

import lombok.Getter;
import lombok.Setter;
import nic.oad.pdfservice.model.domain.EmpMast;

import javax.persistence.*;

@Entity
@Table(name = "emp_master", schema = "oad")
@Getter
@Setter
public class EmpMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Integer empId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nic_emp_code", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private EmpMast empMast;

    @Column(name = "emp_name")
    private String empName;

    @Column(name = "emp_email")
    private String empEmail;

    @Column(name = "emp_mobile")
    private String empMobile;

    @Column(name = "is_tech")
    private Boolean tech;

    @Column(name = "is_outsourced")
    private Boolean outsourced;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "emp_desg")
    private String empDesg;
}
