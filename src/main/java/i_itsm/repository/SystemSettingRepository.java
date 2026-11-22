package i_itsm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import i_itsm.model.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
