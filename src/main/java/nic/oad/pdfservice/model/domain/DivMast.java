package nic.oad.pdfservice.model.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


/**
 * The persistent class for the div_mast database table.
 * 
 */
@Entity
@Table(name="div_mast",schema="public")
@Getter @Setter
class DivMast implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="div_code")
	private String divCode;

	@Column(name="div_name")
	private String divName;

    @Column(name="valid_div")
	private String validDiv;


}