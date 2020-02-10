package nic.oad.pdfservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignProperty implements Serializable {
    List<Integer> parallelSignNumbers;
    String showRemarks;


}
