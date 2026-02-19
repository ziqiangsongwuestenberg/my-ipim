package com.song.my_pim.repository.delivery;

import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryTargetRepository extends JpaRepository<DeliveryTargetEntity, Long> {

    List<DeliveryTargetEntity> findByClientIdAndEnabledTrue(Integer clientId);

    List<DeliveryTargetEntity> findByClientIdAndTypeAndEnabledTrue(Integer clientId,
                                                                    DeliveryTargetType type);
    List<DeliveryTargetEntity> findByEnabledTrue();
}
