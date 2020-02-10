package nic.oad.pdfservice.model.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;


/**
 * The persistent class for the admin_pers_mast database table.
 */
@Entity
@Table(name = "admin_pers_mast", schema = "public")
@Getter @Setter
public class EmpMast implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "emp_code")
    private Long empCode;

    @ManyToOne
    @JoinColumn(name = "desg_code", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private DesgMast desgCode;

    @ManyToOne
    @JoinColumn(name = "div_code",foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private DivMast divCode;

    @Column(name = "emp_name")
    private String empName;

    @Column(name = "emp_name_hindi")
    private String empNameHindi;

    @Column(name = "emp_status")
    private Integer empStatus;

    @Column(name = "emp_title")
    private String empTitle;

    @Column(name = "full_name")
    private String fullName;

    @Temporal(TemporalType.DATE)
    @Column(name = "join_govt")
    private Date joinGovt;

    @Temporal(TemporalType.DATE)
    @Column(name = "join_nic")
    private Date joinNic;

    @Temporal(TemporalType.DATE)
    @Column(name = "retire_date")
    private Date retireDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmpMast that = (EmpMast) o;
        return Objects.equals(empCode, that.empCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(empCode);
    }
}