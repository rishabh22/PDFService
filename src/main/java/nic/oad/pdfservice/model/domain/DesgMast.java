package nic.oad.pdfservice.model.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


/**
 * The persistent class for the desg_mast database table.
 * 
 */
@Entity
@Table(name="desg_mast",schema="public")
@Getter @Setter
class DesgMast implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="desg_code")
	private long desgCode;

	@Column(name="desg_desc")
	private String desgDesc;

	@Column(name="valid_desg")
	private String validDesg;


	
	

}