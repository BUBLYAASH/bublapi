@FilterDef(name = "clinicFilter", defaultCondition = "clinic_id = :clinicId", parameters = @ParamDef(name = "clinicId", type = UUID.class, resolver = ClinicIdResolver.class), autoEnabled = true, applyToLoadByKey = true)
package org.bublapi.dent.common.filter;

import org.bublapi.dent.common.context.ClinicIdResolver;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;
