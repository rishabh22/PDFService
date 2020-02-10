package nic.oad.pdfservice.model.domain.digitalnic;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import nic.oad.pdfservice.model.domain.EmpMast;
import nic.oad.pdfservice.model.dto.SignProperty;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "pdf_service_esign_logs", schema = "digitalnic")
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
@Getter @Setter
public class PdfServiceEsignLogs {
    @Id
    @SequenceGenerator(schema = "digitalnic", name = "pdf_service_esign_log_seq_gen", sequenceName = "pdf_service_esign_log_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pdf_service_esign_log_seq_gen")
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

    private String theme;

    private String host;

    private String referer;

    @Column(name = "signature_count")
    private Integer signatureCount;

    @Column(name = "sign_remark_required")
    private Boolean signRemarkRequired;

    @Column(name = "show_header_footer")
    private Boolean showHeaderFooter;

    @Column(name = "sign_alignment")
    private String signAlignment;

    @Column(name = "sign_type")
    private String signType;

    @Type(type = "jsonb")
    @Column(name = "sign_properties", columnDefinition = "json")
    private Map<Integer, SignProperty> signProperties;
}
