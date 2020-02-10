package nic.oad.pdfservice.dao;

import nic.oad.pdfservice.model.domain.oad.IpMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IpMasterRepo extends JpaRepository<IpMaster, Integer> {
    List<IpMaster> findByPublicIpOrPrivateIp(String publicIp, String privateIp);
}
