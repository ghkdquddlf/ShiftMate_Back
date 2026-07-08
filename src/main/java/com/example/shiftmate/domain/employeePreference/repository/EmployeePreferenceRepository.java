package com.example.shiftmate.domain.employeePreference.repository;

import com.example.shiftmate.domain.employeePreference.entity.EmployeePreference;
import com.example.shiftmate.domain.employeePreference.entity.PreferenceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeePreferenceRepository extends JpaRepository<EmployeePreference, Long> {


    boolean existsByMemberId(Long memberId);

    List<EmployeePreference> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

//    @Query("SELECT ep FROM EmployeePreference ep " +
//               "JOIN FETCH ep.member " +
//               "JOIN FETCH ep.shiftTemplate " +
//               "WHERE ep.type <> :type " +
//               "AND ep.shiftTemplate.id IN :templateIds")
    List<EmployeePreference> findByTypeNotAndShiftTemplate_IdIn(
        @Param("type") PreferenceType preferenceType, @Param("templateIds") List<Long> templateIds);

    void deleteByMemberIdIn(List<Long> memberIds);
}
