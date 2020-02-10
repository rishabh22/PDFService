package nic.oad.pdfservice.model.domain.oad;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "ip_master", schema = "oad")
@Getter
@Setter
public class IpMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "private_ip")
    private String privateIp;

    @Column(name = "public_ip")
    private String publicIp;

    @Column(name = "ip_type")
    private String ipType;

    @Column(name = "is_active")
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "owner_emp_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private EmpMaster owner;

    private String remarks;
}
