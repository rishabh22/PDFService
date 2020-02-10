package nic.oad.pdfservice.model.domain.digitalnic;

import lombok.Getter;
import lombok.Setter;
import nic.oad.pdfservice.model.domain.EmpMast;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "pdf_service_logs", schema = "digitalnic")
@Getter @Setter
//@Profile("dev")
public class PdfServiceLogs {
    @Id
    @SequenceGenerator(schema = "digitalnic", name = "pdf_service_log_seq_gen", sequenceName = "pdf_service_log_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pdf_service_log_seq_gen")
    @Column(name = "log_id", columnDefinition = "bigint")
    private BigInteger logId;

    @ManyToOne
    @JoinColumn(name = "emp_code",foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private EmpMast empCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "log_timestamp", insertable = false, columnDefinition = "timestamp without time zone default now()")
    //@Transient
    private Date logTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "file_timestamp")
    private Date fileTimestamp;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "orientation", length = 1)
    private String orientation;

    private Integer status;

    @Column(name = "error_desc")
    private String errorDesc;

    @Column(name = "file_name")
    private String fileName;

    private String theme;

    @Column(name = "user_agent")
    private String userAgent;

    private String host;

    private String referer;

    @Column(name = "show_header_footer")
    private Boolean showHeaderFooter;

    private Boolean confidential;
}
