/*
 *    eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) 2017  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *            Further, all user interfaces, including but not limited to citizen facing interfaces,
 *            Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *            derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *            For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *            For any further queries on attribution, including queries on brand guidelines,
 *            please contact contact@egovernments.org
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 *
 */
package org.egov.services.report;


import org.apache.log4j.Logger;
import org.egov.egf.model.CommonReportBean;
import org.egov.egf.model.FunctionwiseIE;
import org.egov.egf.model.FunctionwiseIEEntry;
import org.egov.egf.model.ReportSearch;
import org.egov.infra.admin.master.entity.Department;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.egov.infra.exception.ApplicationException;
import org.egov.infstr.services.PersistenceService;
import org.egov.utils.Constants;
import org.egov.utils.FinancialConstants;
import org.hibernate.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FunctionwiseIEService
{
    
 @Autowired
 @Qualifier("persistenceService")
 private PersistenceService persistenceService;
 @Autowired AppConfigValueService appConfigValuesService;
    protected SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Constants.LOCALE);
    protected SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Constants.LOCALE);
    private static final Logger LOGGER = Logger.getLogger(FunctionwiseIEService.class);
    private String capExpCodeCond = "";
    private String capExpCodesWithQuotesCond = "";

	public String getFilterQueryVoucher(final ReportSearch reportSearch, final Map<String, Object> queryParams)
			throws ApplicationException, ParseException {

		final String excludeStatus = appConfigValuesService
				.getConfigValuesByModuleAndKey("finance", "statusexcludeReport").get(0).getValue();
		final StringBuilder appendQry = new StringBuilder(" AND vh.voucherdate>=TO_DATE(:vhFromDate) ");
		appendQry.append(" AND vh.voucherdate<=TO_DATE(:vhToDate) ");
		queryParams.put("vhFromDate", formatter.format(sdf.parse(reportSearch.getStartDate())));
		queryParams.put("vhToDate", formatter.format(sdf.parse(reportSearch.getEndDate())));
		getFiltersExcludingDate(reportSearch, excludeStatus, appendQry, queryParams);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("appendQry==" + appendQry);
		return appendQry.toString();
	}

	public String getFilterQueryVoucherAsOnDate(final ReportSearch reportSearch, final Map<String, Object> queryParams)
			throws ApplicationException, ParseException {

		final String excludeStatus = appConfigValuesService
				.getConfigValuesByModuleAndKey("finance", "statusexcludeReport").get(0).getValue();
		StringBuilder appendQry = new StringBuilder(" AND vh.voucherdate>=TO_DATE(:vhFromDate) ");
		appendQry.append(" AND vh.voucherdate<=TO_DATE(:vhAsOnDate) ");
		queryParams.put("vhFromDate", formatter.format(reportSearch.getYearStartDate()));
		queryParams.put("vhAsOnDate", formatter.format(reportSearch.getAsOnDate()));
		getFiltersExcludingDate(reportSearch, excludeStatus, appendQry, queryParams);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("appendQry==" + appendQry);
		return appendQry.toString();
	}

	public String getFilterQueryVoucherAsOnPreviousYearDate(final ReportSearch reportSearch,
			final Map<String, Object> queryParams) throws ApplicationException, ParseException {

		final String excludeStatus = appConfigValuesService
				.getConfigValuesByModuleAndKey("finance", "statusexcludeReport").get(0).getValue();
		StringBuilder appendQry = new StringBuilder(" AND vh.voucherdate>=TO_DATE(:vhFromDate) ");

		appendQry.append(" AND vh.voucherdate<=TO_DATE(:vhToDate) ");
		queryParams.put("vhFromDate", formatter.format(reportSearch.getPreviousYearStartDate()));
		queryParams.put("vhToDate", formatter.format(reportSearch.getPreviousYearDate()));
		appendQry = getFiltersExcludingDate(reportSearch, excludeStatus, appendQry, queryParams);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("appendQry==" + appendQry);
		return appendQry.toString();
	}

	private StringBuilder getFiltersExcludingDate(final ReportSearch reportSearch, final String excludeStatus,
			StringBuilder appendQry, Map<String, Object> queryParams) {
		appendQry.append(" AND vh.status NOT IN (:excludeStatus)");
		queryParams.put("excludeStatus", excludeStatus);
		if (reportSearch.getFund() != null && reportSearch.getFund().getId() != null) {
			appendQry.append(" AND vh.fundid =:fundid");
			queryParams.put("fundid", reportSearch.getFund().getId());
		}
		if (reportSearch.getFundsource() != null && reportSearch.getFundsource().getId() != null) {
			appendQry.append(" AND vh.fundsourceid = :fundsourceid");
			queryParams.put("fundsourceid", reportSearch.getFundsource().getId());
		}
		if (reportSearch.getDepartment() != null && reportSearch.getDepartment().getId() != null) {
			appendQry.append(" AND vmis.departmentid = :departmentid");
			queryParams.put("departmentid", reportSearch.getDepartment().getId());
		}
		if (reportSearch.getField() != null && reportSearch.getField().getId() != null) {
			appendQry.append(" AND vmis.divisionid = :divisionid");
			queryParams.put("divisionid", reportSearch.getField().getId());
		}
		if (reportSearch.getScheme() != null && reportSearch.getScheme().getId() != null) {
			appendQry.append(" AND vmis.schemeid = :schemeid");
			queryParams.put("schemeid", reportSearch.getScheme().getId());
		}
		if (reportSearch.getSubScheme() != null && reportSearch.getSubScheme().getId() != null) {
			appendQry.append(" AND vmis.subschemeid =:subschemeid");
			queryParams.put("subschemeid", reportSearch.getSubScheme().getId());
		}
		if (reportSearch.getFunctionary() != null && reportSearch.getFunctionary().getId() != null) {
			appendQry.append(" AND vmis.functionaryid =:functionaryid");
			queryParams.put("functionaryid", reportSearch.getFunctionary().getId());
		}
		return appendQry;
	}

	public String getFilterQueryGL(final ReportSearch reportSearch, final Map<String, Object> queryParams) {
		StringBuilder appendQry = new StringBuilder();
		if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null) {
			appendQry.append(" AND gl.functionid = :functionid");
			queryParams.put("queryParams", reportSearch.getFunction().getId());
		}
		return appendQry.toString();
	}

	public void getMajorCodeList(final FunctionwiseIE functionwiseIE, final ReportSearch reportSearch)
			throws ApplicationException, ParseException {

		final List<String> majorCodeList = new ArrayList<>();
		final Map<String, Object> queryParams = new HashMap<>();
		final String filterQuery = getFilterQueryVoucher(reportSearch, queryParams);
		final StringBuilder sql = new StringBuilder("select distinct ")
				.append(String.format("SUBSTR(gl.glcode,1,%d)", reportSearch.getMinorCodeLen()))
				.append(",coa.name from CHARTOFACCOUNTS coa,GENERALLEDGER gl WHERE gl.functionid is not null")
				.append(" and gl.voucherheaderid IN (SELECT vh.id FROM VOUCHERHEADER vh,vouchermis vmis")
				.append(" WHERE vh.id=vmis.voucherheaderid ").append(filterQuery)
				.append(" AND coa.TYPE=:coaType AND SUBSTR(gl.glcode,1,:majorCodeLength)=coa.glcode) ")
				.append(getFilterQueryGL(reportSearch, queryParams)).append(" ORDER BY 1");

		queryParams.put("queryParams", reportSearch.getIncExp());
		queryParams.put("majorCodeLength", reportSearch.getMajorCodeLen());

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("sql====================" + sql);
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString());
		queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
		final List<Object[]> list = query.list();
		for (final Object[] obj : list)
			majorCodeList.add(obj[0].toString() + "-" + obj[1].toString());
		functionwiseIE.setMajorCodeList(majorCodeList);
	}

	public List<String> getMinorCodeList(final ReportSearch reportSearch) throws ApplicationException, ParseException {
		final List<String> minorCodeList = new ArrayList<>();
		final Map<String, Object> queryParams = new HashMap<>();
		final String filterQuery = getFilterQueryVoucher(reportSearch, queryParams);
		final StringBuilder sql = new StringBuilder("select distinct ")
				.append(String.format("SUBSTR(gl.glcode,1,%d)", reportSearch.getMinorCodeLen()))
				.append(",coa.name from CHARTOFACCOUNTS coa,GENERALLEDGER gl WHERE gl.functionid is not null")
				.append(" and gl.voucherheaderid IN (SELECT vh.id FROM VOUCHERHEADER vh,vouchermis vmis")
				.append(" WHERE vh.id=vmis.voucherheaderid ").append(filterQuery)
				.append(" AND coa.TYPE=:coaType AND SUBSTR(gl.glcode,1,:majorCodeLength)=coa.glcode) ")
				.append(getFilterQueryGL(reportSearch, queryParams)).append(" ORDER BY 1");
		queryParams.put("coaType", reportSearch.getIncExp());
		queryParams.put("majorCodeLength", reportSearch.getMinorCodeLen());
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("sql====================" + sql);
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString());
		queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
		final List<Object[]> list = query.list();
		for (final Object[] obj : list)
			minorCodeList.add(obj[0].toString() + "-" + obj[1].toString());
		return minorCodeList;
	}

    /**
     *
     * @param reportSearch
     * @return
     * @throws ApplicationException
     * @throws ParseException for Main report getMajor Code and Minor Code for Sub Report get only minor code
     */
	public List<CommonReportBean> getMinorAndMajorCodeList(final ReportSearch reportSearch)
			throws ApplicationException, ParseException {
		final StringBuilder sql = new StringBuilder();
		if (reportSearch.getByDepartment() && reportSearch.getByDetailCode()) {
			sql.append(
					" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId,0 as isMajor")
					.append(" from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:minorCodeLen and coa.glcode like :glcode")
					.append(" and classification=4 and isActiveForPosting=true order by 1 ");
		} else if (reportSearch.getByDepartment()) {
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append("0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" and coa.FIEscheduleId=:FIEscheduleId  order by 1");
		} else {
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append(" 0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" Union ")
					.append(" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId ,1 as isMajor")
					.append(" from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:majorCodeLen").append(" order by 1");
		}
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
				.addScalar("accCode", StringType.INSTANCE).addScalar("name", StringType.INSTANCE)
				.addScalar("schedule", StringType.INSTANCE).addScalar("FIEscheduleId", LongType.INSTANCE)
				.addScalar("isMajor", BooleanType.INSTANCE).setString("type", reportSearch.getIncExp())
				.setParameter("minorCodeLen", reportSearch.getMinorCodeLen())
				.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
		if (reportSearch.getByDetailCode()) {
			query.setString("glcode", reportSearch.getGlcode() + "%");
		} else if (reportSearch.getByDepartment()) {
			query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
		} else {
			query.setParameter("majorCodeLen", reportSearch.getMajorCodeLen());
		}
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("----------------" + sql);
		return query.list();

	}

    /**
     *
     * @param reportSearch
     * @return
     * @throws ApplicationException
     * @throws ParseException for Main report getMajor Code and Minor Code for Sub Report get only minor code
     */
	public List<CommonReportBean> getMinorAndMajorCodeListForCapitalExp(final ReportSearch reportSearch)
			throws ApplicationException, ParseException {
		final StringBuilder sql = new StringBuilder();
		if (reportSearch.getByDepartment() && reportSearch.getByDetailCode())
			sql.append(
					" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId,0 as isMajor")
					.append(" from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" and coa.glcode like :glcode and classification=4 and isActiveForPosting=true order by 1 ");
		else if (reportSearch.getByDepartment())
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append("0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" and coa.FIEscheduleId=:FIEscheduleId  order by 1");
		else
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append(" 0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" Union ")
					.append(" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId ,1 as isMajor")
					.append(" from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:majorCodeLen")
					.append(" and coa.glcode in (:capExpCodesWithQuotesCond)").append(" order by 1");
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
				.addScalar("accCode", StringType.INSTANCE).addScalar("name", StringType.INSTANCE)
				.addScalar("schedule", StringType.INSTANCE).addScalar("FIEscheduleId", LongType.INSTANCE)
				.addScalar("isMajor", BooleanType.INSTANCE).setString("type", "A")
				.setParameter("minorCodeLen", reportSearch.getMinorCodeLen())
				.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
		if (reportSearch.getByDetailCode())
			query.setString("glcode", reportSearch.getGlcode() + "%");
		else if (reportSearch.getByDepartment())
			query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
		else {
			query.setParameter("capExpCodesWithQuotesCond", capExpCodesWithQuotesCond);
			query.setParameter("majorCodeLen", reportSearch.getMajorCodeLen());
		}
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("----------------" + sql);
		return query.list();

	}

	public List<CommonReportBean> getIncomeMinorAndMajorCodeList(final ReportSearch reportSearch)
			throws ApplicationException, ParseException {
		final StringBuilder sql = new StringBuilder();
		if (reportSearch.getByDepartment() && reportSearch.getByDetailCode())
			sql.append(
					" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId,0 as isMajor")
					.append(" from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" and coa.glcode like :glcode and classification=4 and isActiveForPosting=true order by 1 ");
		else if (reportSearch.getByDepartment())
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append("0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" and coa.FIEscheduleId=:FIEscheduleId  order by 1");
		else
			sql.append(" select coa.glcode as accCode,coa.name as name,mp.schedule as schedule,mp.id as FIEscheduleId,")
					.append(" 0 as isMajor from Chartofaccounts coa,Schedulemapping mp")
					.append(" where coa.FIEscheduleId=mp.id and coa.type=:type and length(coa.glcode)=:minorCodeLen")
					.append(" Union ")
					.append(" select coa.glcode as accCode,coa.name as name,null as schedule,null as FIEscheduleId ,1 as isMajor ")
					.append("from Chartofaccounts coa")
					.append(" where  coa.type=:type and length(coa.glcode)=:majorCodeLen").append(" order by 1");
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
				.addScalar("accCode", StringType.INSTANCE).addScalar("name", StringType.INSTANCE)
				.addScalar("schedule", StringType.INSTANCE).addScalar("FIEscheduleId", LongType.INSTANCE)
				.addScalar("isMajor", BooleanType.INSTANCE).setString("type", reportSearch.getIncExp())
				.setParameter("minorCodeLen", reportSearch.getMinorCodeLen())
				.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
		if (reportSearch.getByDetailCode())
			query.setString("glcode", reportSearch.getGlcode() + "%");
		else if (reportSearch.getByDepartment())
			query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
		else
			query.setParameter("majorCodeLen", reportSearch.getMajorCodeLen());
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("----------------" + sql);
		return query.list();

	}

	public void getAmountList(final FunctionwiseIE functionwiseIE, final ReportSearch reportSearch)
			throws ApplicationException, ParseException {
		final Map<String, Object> queryParams = new HashMap<>();
		final StringBuilder sql = new StringBuilder(
				"SELECT fn.code,fn.name,CONCAT(CONCAT(coa.majorcode,'-'),coa.name),")
						.append(String.format("case %s", reportSearch.getIncExp()))
						.append(" when  'I' then (SUM(gl.creditamount)-SUM(gl.debitamount)) when 'E'")
						.append(" then (SUM(gl.debitamount)-SUM(gl.creditamount)) else 0 end AS amt ")
						.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:majorCodeLen)")
						.append(" =coa.glcode AND coa.TYPE= :coaType AND fn.id = gl.functionid ")
						.append(getFilterQueryVoucher(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(" GROUP BY fn.code,fn.name,CONCAT(CONCAT(coa.majorcode,'-'),coa.name) order by 1,3");
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("sql===" + sql);
		final Query query = persistenceService.getSession().createSQLQuery(sql.toString());
		query.setParameter("majorCodeLen", reportSearch.getMajorCodeLen()).setParameter("coaType",
				reportSearch.getIncExp());

		queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));

		final List<Object[]> list = query.list();
		FunctionwiseIEEntry entry = new FunctionwiseIEEntry();
		Map<String, BigDecimal> majorcodeWiseAmount = new HashMap<String, BigDecimal>();
		final Map<String, BigDecimal> totalAmountMap = new HashMap<String, BigDecimal>();
		String tempFunctionCode = "";
		BigDecimal totalIncome = BigDecimal.ZERO;
		BigDecimal grandTotal = BigDecimal.ZERO;
		int i = 1;
		for (final Object[] obj : list) {
			if (tempFunctionCode.equals(obj[0].toString())) {
				if (functionwiseIE.getMajorCodeList().contains(obj[2].toString())) {
					majorcodeWiseAmount.put(obj[2].toString(), round((BigDecimal) obj[3]));
					totalIncome = totalIncome.add((BigDecimal) obj[3]);
				}
			} else {
				if (!majorcodeWiseAmount.isEmpty()) {
					entry.setTotalIncome(round(totalIncome));
					entry.setMajorcodeWiseAmount(majorcodeWiseAmount);
					functionwiseIE.add(entry);
					totalIncome = BigDecimal.ZERO;
				}

				entry = new FunctionwiseIEEntry();
				entry.setSlNo(String.valueOf(i++));
				entry.setFunctionCode(obj[0].toString());
				entry.setFunctionName(obj[1].toString());
				majorcodeWiseAmount = new HashMap<String, BigDecimal>();
				if (functionwiseIE.getMajorCodeList().contains(obj[2].toString())) {
					majorcodeWiseAmount.put(obj[2].toString(), round((BigDecimal) obj[3]));
					totalIncome = totalIncome.add((BigDecimal) obj[3]);
				}
			}
			if (totalAmountMap.containsKey(obj[2].toString()))
				totalAmountMap.put(obj[2].toString(), totalAmountMap.get(obj[2].toString()).add((BigDecimal) obj[3]));
			else
				totalAmountMap.put(obj[2].toString(), (BigDecimal) obj[3]);
			grandTotal = grandTotal.add((BigDecimal) obj[3]);
			tempFunctionCode = obj[0].toString();
		}
		if (!majorcodeWiseAmount.isEmpty()) {
			entry.setTotalIncome(round(totalIncome));
			entry.setMajorcodeWiseAmount(majorcodeWiseAmount);
			functionwiseIE.add(entry);

			entry = new FunctionwiseIEEntry();
			entry.setSlNo("");
			entry.setFunctionName("Total for the Period");
			entry.setTotalIncome(round(grandTotal));
			majorcodeWiseAmount = new HashMap<String, BigDecimal>();
			final Iterator it = totalAmountMap.keySet().iterator();
			String key;
			while (it.hasNext()) {
				key = it.next().toString();
				majorcodeWiseAmount.put(key, round(totalAmountMap.get(key)));
			}
			entry.setMajorcodeWiseAmount(majorcodeWiseAmount);
			functionwiseIE.add(entry);
		}
	}

	@SuppressWarnings("unchecked")
	public List<CommonReportBean> getAmountListForMinorCode(final FunctionwiseIE functionwiseIE,
			final ReportSearch reportSearch) throws ApplicationException, ParseException {

		final StringBuilder sql = new StringBuilder();
		final Map<String, Object> queryParams = new HashMap<>();
		List<CommonReportBean> list = null;

		if (reportSearch.getByDetailCode()) {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d)", reportSearch.getMinorCodeLen())).append(
						" as accCode,d.dept_name as deptName ,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount,0 as isMajor ");
			else
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d)", reportSearch.getMinorCodeLen())).append(
						" as accCode,d.dept_name as deptName ,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount,0 as isMajor ");

			if (reportSearch.getIncExp().equalsIgnoreCase("E")) {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,")
						.append("vouchermis vmis,eg_department d  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen)")
						.append("=coa.glcode AND (coa.TYPE=:coType").append(capExpCodeCond).append(")")
						.append(" and d.dept_name=:deptName and coa.glcode like :glcode AND fn.id = gl.functionid")
						.append(" and vmis.departmentid=d.id_dept ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),d.dept_name ");
				queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
				queryParams.put("coType", reportSearch.getIncExp());
			} else {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,")
						.append("vouchermis vmis,eg_department d  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
						.append(")=coa.glcode AND coa.TYPE=:coaType")
						.append(" and d.dept_name=:deptName and coa.glcode like :glcode AND fn.id = gl.functionid ")
						.append(" and vmis.departmentid=d.id_dept ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),d.dept_name ");
				queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
				queryParams.put("coType", reportSearch.getIncExp());
			}
			sql.append("order by 2,1 ");
			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE).addScalar("deptName", StringType.INSTANCE)
					.setString("glcode", reportSearch.getGlcode() + "%")
					.setString("deptName", reportSearch.getDepartment().getName())
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();

		} else if (reportSearch.getByDepartment()) {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount,0 as isMajor ");
			else
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount,0 as isMajor ");

			if (reportSearch.getIncExp().equalsIgnoreCase("E")) {
				sql.append(
						" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis,eg_department d  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
						.append(")=coa.glcode AND (coa.TYPE=:coaType").append(capExpCodeCond).append(")")
						.append(" AND fn.id = gl.functionid  and vmis.departmentid=d.id_dept ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),d.dept_name ");

				queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
				queryParams.put("coaType", reportSearch.getIncExp());
			} else {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,")
						.append("vouchermis vmis,eg_department d  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
						.append(")=coa.glcode AND coa.TYPE=:coaType")
						.append(" AND fn.id = gl.functionid  and vmis.departmentid=d.id_dept ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),d.dept_name ");
			}
			sql.append("order by 2,1 ");
			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE).addScalar("deptName", StringType.INSTANCE)
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();

		} else {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append("SELECT coa.majorcode as accCode,coa.name as accName,(SUM(gl.debitamount)-")
						.append("SUM(gl.creditamount)) AS amount,1 as isMajor ");
			else
				sql.append("SELECT coa.majorcode as accCode,coa.name as accName,(SUM(gl.creditamount)-")
						.append("SUM(gl.debitamount)) AS amount,1 as isMajor ");

			if (reportSearch.getIncExp().equalsIgnoreCase("E")) {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:majorCodeLen")
						.append(")=coa.glcode AND (coa.TYPE=:coaType").append(capExpCodeCond).append(")")
						.append(" AND fn.id = gl.functionid ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(" GROUP BY coa.majorcode,coa.name ");
				queryParams.put("majorCodeLen", reportSearch.getMajorCodeLen());
				queryParams.put("coaType", reportSearch.getIncExp());
			} else {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis  ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:majorCodeLen")
						.append(")=coa.glcode AND coa.TYPE=:coaType").append(" AND fn.id = gl.functionid ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(" GROUP BY coa.majorcode,coa.name ");
				queryParams.put("majorCodeLen", reportSearch.getMajorCodeLen());
				queryParams.put("coaType", reportSearch.getIncExp());
			}
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format(" Union SELECT SUBSTR(coa.glcode,1,", reportSearch.getMinorCodeLen())).append(
						") as accCode, coa.name  as accName,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount ,0 as isMajor ");
			else
				sql.append(String.format(" Union SELECT SUBSTR(coa.glcode,1,", reportSearch.getMinorCodeLen())).append(
						") as accCode, coa.name  as accName,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount ,0 as isMajor ");

			if (reportSearch.getIncExp().equalsIgnoreCase("E")) {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
						.append(")=coa.glcode AND (coa.TYPE=:coaType").append(capExpCodeCond).append(")")
						.append(" AND fn.id = gl.functionid ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),coa.name order by 1,2 ");
				queryParams.put("majorCodeLen", reportSearch.getMinorCodeLen());
				queryParams.put("coaType", reportSearch.getIncExp());
			} else {
				sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis ")
						.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
						.append(")=coa.glcode AND coa.TYPE=:coaType").append(" AND fn.id = gl.functionid ")
						.append(getFilterQueryVoucherAsOnDate(reportSearch, queryParams))
						.append(getFilterQueryGL(reportSearch, queryParams))
						.append(String.format(" GROUP BY SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append("),coa.name order by 1,2 ");
				queryParams.put("majorCodeLen", reportSearch.getMinorCodeLen());
				queryParams.put("coaType", reportSearch.getIncExp());
			}
			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE)
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));

			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();
		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("sql===" + sql);
		return list;

	}

	@SuppressWarnings("unchecked")
	public List<CommonReportBean> getPreviousYearAmountListForMinorCode(final FunctionwiseIE functionwiseIE,
			final ReportSearch reportSearch) throws ApplicationException, ParseException {

		final StringBuilder sql = new StringBuilder();
		final Map<String, Object> queryParams = new HashMap<>();
		List<CommonReportBean> list = null;

		if (reportSearch.getByDetailCode()) {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount,0 as isMajor ");
			else
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount,0 as isMajor ");

			sql.append(
					" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis,eg_department d  ")
					.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
					.append(")=coa.glcode AND coa.TYPE=:coaType")
					.append(" and d.dept_name=:deptName and coa.glcode like :glcode AND fn.id = gl.functionid ")
					.append(" and vmis.departmentid=d.id_dept ")
					.append(getFilterQueryVoucherAsOnPreviousYearDate(reportSearch, queryParams))
					.append(getFilterQueryGL(reportSearch, queryParams))
					.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
					.append("),d.dept_name ");
			queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
			queryParams.put("coaType", reportSearch.getIncExp());

			sql.append("order by 2,1 ");
			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE).addScalar("deptName", StringType.INSTANCE)
					.setString("glcode", reportSearch.getGlcode() + "%")
					.setString("deptName", reportSearch.getDepartment().getName())
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();
		} else if (reportSearch.getByDepartment()) {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount,0 as isMajor ");
			else
				sql.append(String.format("SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(
						") as accCode,d.dept_name as deptName ,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount,0 as isMajor ");

			sql.append(
					" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis,eg_department d  ")
					.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
					.append(")=coa.glcode AND coa.TYPE=:coaType")
					.append(" AND fn.id = gl.functionid  and vmis.departmentid=d.id_dept ")
					.append(getFilterQueryVoucherAsOnPreviousYearDate(reportSearch, queryParams))
					.append(getFilterQueryGL(reportSearch, queryParams))
					.append(String.format(" GROUP BY  SUBSTR(coa.glcode,1,%d", +reportSearch.getMinorCodeLen()))
					.append("),d.dept_name ");

			queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
			queryParams.put("coaType", reportSearch.getIncExp());

			sql.append(" order by 2,1 ");
			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE).addScalar("deptName", StringType.INSTANCE)
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();
		} else {
			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append("SELECT coa.majorcode as accCode,coa.name as accName,(SUM(gl.debitamount)-")
						.append("SUM(gl.creditamount)) AS amount,1 as isMajor ");
			else
				sql.append("SELECT coa.majorcode as accCode,coa.name as accName,(SUM(gl.creditamount)-")
						.append("SUM(gl.debitamount)) AS amount,1 as isMajor ");
			sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis  ")
					.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:majorCodeLen")
					.append(")=coa.glcode AND coa.TYPE=:coaType").append(" AND fn.id = gl.functionid ")
					.append(getFilterQueryVoucherAsOnPreviousYearDate(reportSearch, queryParams))
					.append(getFilterQueryGL(reportSearch, queryParams)).append(" GROUP BY coa.majorcode,coa.name ");

			queryParams.put("majorCodeLen", reportSearch.getMajorCodeLen());
			queryParams.put("coaType", reportSearch.getIncExp());

			if (reportSearch.getIncExp().equalsIgnoreCase("E"))
				sql.append(String.format(" Union SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append(") as accCode, coa.name  as accName,(SUM(gl.debitamount)-SUM(gl.creditamount)) AS amount ,0 as isMajor ");
			else
				sql.append(String.format(" Union SELECT SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
						.append(") as accCode, coa.name  as accName,(SUM(gl.creditamount)-SUM(gl.debitamount)) AS amount ,0 as isMajor ");

			sql.append(" FROM GENERALLEDGER gl,FUNCTION fn,VOUCHERHEADER vh, CHARTOFACCOUNTS coa,vouchermis vmis ")
					.append(" WHERE vh.id=vmis.voucherheaderid and vh.ID=gl.voucherheaderid AND SUBSTR(gl.glcode,1,:minorCodeLen")
					.append(")=coa.glcode AND coa.TYPE=:coaType").append(" AND fn.id = gl.functionid ")
					.append(getFilterQueryVoucherAsOnPreviousYearDate(reportSearch, queryParams))
					.append(getFilterQueryGL(reportSearch, queryParams))
					.append(String.format(" GROUP BY SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
					.append("),coa.name order by 1,2 ");

			queryParams.put("minorCodeLen", reportSearch.getMinorCodeLen());
			queryParams.put("coaType", reportSearch.getIncExp());

			final Query query = persistenceService.getSession().createSQLQuery(sql.toString())
					.addScalar("accCode", StringType.INSTANCE).addScalar("amount", BigDecimalType.INSTANCE)
					.addScalar("isMajor", BooleanType.INSTANCE)
					.setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
			queryParams.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			list = query.list();
		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("sql===" + sql);
		return list;

	}

    public void populateData(final FunctionwiseIE functionwiseIE, final ReportSearch reportSearch) throws ApplicationException,
    ParseException
    {
        getMajorCodeList(functionwiseIE, reportSearch);
        getAmountList(functionwiseIE, reportSearch);
    }

    public List<CommonReportBean> populateDataWithBudget(final FunctionwiseIE functionwiseIE, final ReportSearch reportSearch)
            throws ApplicationException, ParseException
            {
        final String capExpCode = appConfigValuesService.getConfigValuesByModuleAndKey
                (Constants.EGF, FinancialConstants.APPCONFIG_COA_MAJORCODE_CAPITAL_EXP_FIE_REPORT).get(0).getValue();
        final String[] temp = capExpCode.split(",");
        // To generate condition for appconfig values.
        String capExpCodes = "";
        for (final String element : temp)
            capExpCodes = capExpCodes + " or coa.glcode like '" + element + "%'";
        capExpCodeCond = capExpCodes;
        // To generate major code values from appconfig with quotes.
        String capExpCodesWithQuotes = "";
        for (int i = 0; i < temp.length; i++) {
            capExpCodesWithQuotes = capExpCodesWithQuotes + "'" + temp[i] + "'";
            if (i != temp.length - 1)
                capExpCodesWithQuotes = capExpCodesWithQuotes + ",";
        }
        capExpCodesWithQuotesCond = capExpCodesWithQuotes;

        // functionwiseIE.setMinorCodeList(getMinorCodeList(reportSearch));
        final List<CommonReportBean> minorAndMajorCodeList = getMinorAndMajorCodeList(reportSearch);
        if (reportSearch.getIncExp().equalsIgnoreCase("E")) {
            final List<CommonReportBean> minorAndMajorCodeListForCapitalExp = getMinorAndMajorCodeListForCapitalExp(reportSearch);
            minorAndMajorCodeList.addAll(minorAndMajorCodeListForCapitalExp);
        }
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("deptName\t Acccode\t Name\t Amount");
        final List<CommonReportBean> amountListForMinorCode = getAmountListForMinorCode(functionwiseIE, reportSearch);
        final List<CommonReportBean> amountPreviousyearListForMinorCode = getPreviousYearAmountListForMinorCode(functionwiseIE,
                reportSearch);
        // if(LOGGER.isDebugEnabled())
        // LOGGER.debug("amountListForMinorCode---------------------------------------------------------------------------------------");
        print(amountListForMinorCode);
        final StringBuilder queryStr = getBudgetQueryForMinorCodes(reportSearch);
        final List<CommonReportBean> beAmountListForMinorCode = getBudgetAmountListForMinorCode(reportSearch, "BE",
                queryStr.toString());
        // if(LOGGER.isDebugEnabled())
        // LOGGER.debug("beAmountListForMinorCode---------------------------------------------------------------------------------------");
        print(beAmountListForMinorCode);
        final List<CommonReportBean> reAmountListForMinorCode = getBudgetAmountListForMinorCode(reportSearch, "RE",
                queryStr.toString());
        // if(LOGGER.isDebugEnabled())
        // LOGGER.debug("reAmountListForMinorCode---------------------------------------------------------------------------------------");
        print(reAmountListForMinorCode);
        final StringBuilder reappQueryStr = getBudgetReappQueryForMinorCodes(reportSearch);
        final List<CommonReportBean> beappAmountListForMinorCode = getBudgetApprAmountListForMinorCode(reportSearch, "BE",
                reappQueryStr.toString());
        final List<CommonReportBean> reappAmountListForMinorCode = getBudgetApprAmountListForMinorCode(reportSearch, "RE",
                reappQueryStr.toString());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("beappAmountListForMinorCode---------------------------------------------------------------------------------------");
        print(beappAmountListForMinorCode);
        // two logic here 1. put to linked hashmap and get
        // 2. merge by comparing
        final Map<String, CommonReportBean> minorCodeAmountMap = loadIntoMap(amountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> beAmountMap = loadIntoMap(beAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> reAmountMap = loadIntoMap(reAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> reAppAmountMap = loadIntoMap(reappAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> beAppAmountMap = loadIntoMap(beappAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> prevousAmountMap = loadIntoMap(amountPreviousyearListForMinorCode, reportSearch);

        new ArrayList<CommonReportBean>();
        CommonReportBean beCRB;
        CommonReportBean reCRB;
        CommonReportBean reAppCRB;
        CommonReportBean beAppCRB;
        CommonReportBean glCRB;
        CommonReportBean pyglCRB;
        BigDecimal beSum = BigDecimal.ZERO, reSum = BigDecimal.ZERO, beAppSum = BigDecimal.ZERO, reAppSum = BigDecimal.ZERO, amountSum = BigDecimal.ZERO, pyAmountSum = BigDecimal.ZERO;
        if (reportSearch.getByDepartment() && !reportSearch.getByDetailCode())
        {
            final List<CommonReportBean> deptWiseWithBudgetList = new ArrayList<CommonReportBean>();
            CommonReportBean crb;

            for (final Department dept : reportSearch.getDeptList())
                for (final CommonReportBean bean : minorAndMajorCodeList)
                {
                    final String accCode = dept.getName() + "-" + bean.getAccCode();
                    crb = new CommonReportBean();

                    crb.setName(bean.getName());
                    crb.setDeptName(dept.getName());
                    crb.setAccCode(bean.getAccCode());
                    glCRB = minorCodeAmountMap.get(accCode);
                    pyglCRB = prevousAmountMap.get(accCode);
                    beCRB = beAmountMap.get(accCode);
                    reCRB = reAmountMap.get(accCode);
                    reAppCRB = reAppAmountMap.get(accCode);
                    beAppCRB = beAppAmountMap.get(accCode);
                    if (glCRB != null)
                    {
                        crb.setAmount(glCRB.getAmount());
                        if (glCRB.getIsMajor() == false)
                            amountSum = glCRB.getAmount() == null ? amountSum.add(BigDecimal.ZERO) : amountSum.add(glCRB
                                    .getAmount());
                    }
                    if (pyglCRB != null)
                    {
                        crb.setPyAmount(pyglCRB.getAmount());
                        if (pyglCRB.getIsMajor() == false)
                            pyAmountSum = pyglCRB.getAmount() == null ? pyAmountSum.add(BigDecimal.ZERO) : pyAmountSum
                                    .add(pyglCRB.getAmount());
                    }
                    if (beCRB != null)
                    {
                        crb.setBeAmount(beCRB.getAmount());
                        if (beCRB.getIsMajor() == false)
                            beSum = beCRB.getAmount() == null ? beSum.add(BigDecimal.ZERO) : beSum.add(beCRB.getAmount());
                    }
                    if (reCRB != null)
                    {
                        crb.setReAmount(reCRB.getAmount());
                        if (reCRB.getIsMajor() == false)
                            reSum = reCRB.getAmount() == null ? reSum.add(BigDecimal.ZERO) : reSum.add(reCRB.getAmount());
                    }
                    if (reAppCRB != null)
                    {
                        crb.setReAppAmount(reAppCRB.getAmount());
                        if (reAppCRB.getIsMajor() == false)
                            reAppSum = reAppCRB.getAmount() == null ? reAppSum.add(BigDecimal.ZERO) : reAppSum.add(reAppCRB
                                    .getAmount());
                    }
                    if (beAppCRB != null)
                    {
                        crb.setBeAppAmount(beAppCRB.getAmount());
                        if (beAppCRB.getIsMajor() == false)
                            beAppSum = beAppCRB.getAmount() == null ? beAppSum.add(BigDecimal.ZERO) : beAppSum.add(beAppCRB
                                    .getAmount());
                    }

                    deptWiseWithBudgetList.add(crb);
                }
            final CommonReportBean totalCrb = new CommonReportBean("", "TOTAL", beSum, reSum, beAppSum, reAppSum, amountSum,
                    pyAmountSum);

            deptWiseWithBudgetList.add(totalCrb);
            // print(deptWiseWithBudgetList);
            return deptWiseWithBudgetList;
        } else
        {
            for (final CommonReportBean crb : minorAndMajorCodeList)
            {
                final String accCode = crb.getAccCode();
                glCRB = minorCodeAmountMap.get(accCode);
                pyglCRB = prevousAmountMap.get(accCode);
                beCRB = beAmountMap.get(accCode);
                reCRB = reAmountMap.get(accCode);
                reAppCRB = reAppAmountMap.get(accCode);
                beAppCRB = beAppAmountMap.get(accCode);
                if (glCRB != null)
                {
                    crb.setAmount(glCRB.getAmount());
                    if (glCRB.getIsMajor() == false)
                        amountSum = glCRB.getAmount() == null ? amountSum.add(BigDecimal.ZERO) : amountSum.add(glCRB.getAmount());
                }
                if (pyglCRB != null)
                {
                    crb.setPyAmount(pyglCRB.getAmount());
                    if (pyglCRB.getIsMajor() == false)
                        pyAmountSum = pyglCRB.getAmount() == null ? pyAmountSum.add(BigDecimal.ZERO) : pyAmountSum.add(pyglCRB
                                .getAmount());
                }
                if (beCRB != null)
                {
                    crb.setBeAmount(beCRB.getAmount());
                    if (beCRB.getIsMajor() == false)
                        beSum = beCRB.getAmount() == null ? beSum.add(BigDecimal.ZERO) : beSum.add(beCRB.getAmount());
                }
                if (reCRB != null)
                {
                    crb.setReAmount(reCRB.getAmount());
                    if (reCRB.getIsMajor() == false)
                        reSum = reCRB.getAmount() == null ? reSum.add(BigDecimal.ZERO) : reSum.add(reCRB.getAmount());
                }
                if (reAppCRB != null)
                {
                    crb.setReAppAmount(reAppCRB.getAmount());
                    if (reAppCRB.getIsMajor() == false)
                        reAppSum = reAppCRB.getAmount() == null ? reAppSum.add(BigDecimal.ZERO) : reAppSum.add(reAppCRB
                                .getAmount());
                }
                if (beAppCRB != null)
                {
                    crb.setBeAppAmount(beAppCRB.getAmount());
                    if (beAppCRB.getIsMajor() == false)
                        beAppSum = beAppCRB.getAmount() == null ? beAppSum.add(BigDecimal.ZERO) : beAppSum.add(beAppCRB
                                .getAmount());
                }
            }
            final CommonReportBean totalCrb = new CommonReportBean("", "TOTAL", beSum, reSum, beAppSum, reAppSum, amountSum,
                    pyAmountSum);
            minorAndMajorCodeList.add(totalCrb);
        }
        // print(minorAndMajorCodeList);
        return minorAndMajorCodeList;
            }

    public List<CommonReportBean> populateIncomeDataWithBudget(final FunctionwiseIE functionwiseIE,
            final ReportSearch reportSearch)
            throws ApplicationException, ParseException
            {

        final List<CommonReportBean> minorAndMajorCodeList = getIncomeMinorAndMajorCodeList(reportSearch);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("deptName\t Acccode\t Name\t Amount");
        final List<CommonReportBean> amountListForMinorCode = getAmountListForMinorCode(functionwiseIE, reportSearch);
        final List<CommonReportBean> amountPreviousyearListForMinorCode = getPreviousYearAmountListForMinorCode(functionwiseIE,
                reportSearch);

        final StringBuilder queryStr = getBudgetQueryForMinorCodes(reportSearch);
        final List<CommonReportBean> beAmountListForMinorCode = getBudgetAmountListForMinorCode(reportSearch, "BE",
                queryStr.toString());
        // if(LOGGER.isDebugEnabled())
        // LOGGER.debug("beAmountListForMinorCode---------------------------------------------------------------------------------------");
        // print(beAmountListForMinorCode);
        final List<CommonReportBean> reAmountListForMinorCode = getBudgetAmountListForMinorCode(reportSearch, "RE",
                queryStr.toString());
        // if(LOGGER.isDebugEnabled())
        // LOGGER.debug("reAmountListForMinorCode---------------------------------------------------------------------------------------");
        // print(reAmountListForMinorCode);
        final Map<String, CommonReportBean> minorCodeAmountMap = loadIntoMap(amountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> beAmountMap = loadIntoMap(beAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> reAmountMap = loadIntoMap(reAmountListForMinorCode, reportSearch);
        final Map<String, CommonReportBean> prevousAmountMap = loadIntoMap(amountPreviousyearListForMinorCode, reportSearch);
        new ArrayList<CommonReportBean>();
        CommonReportBean pyglCRB;
        CommonReportBean glCRB;
        CommonReportBean objBE;
        CommonReportBean objRE;
        BigDecimal beTotal = BigDecimal.ZERO, reTotal = BigDecimal.ZERO, previousSum = BigDecimal.ZERO, amountSum = BigDecimal.ZERO;
        if (reportSearch.getByDepartment() && !reportSearch.getByDetailCode())
        {
            final List<CommonReportBean> deptWiseWithBudgetList = new ArrayList<CommonReportBean>();
            CommonReportBean crb;

            for (final Department dept : reportSearch.getDeptList())
                for (final CommonReportBean bean : minorAndMajorCodeList)
                {
                    final String accCode = dept.getName() + "-" + bean.getAccCode();
                    crb = new CommonReportBean();

                    crb.setName(bean.getName());
                    crb.setDeptName(dept.getName());
                    crb.setAccCode(bean.getAccCode());
                    glCRB = minorCodeAmountMap.get(accCode);
                    pyglCRB = prevousAmountMap.get(accCode);
                    objBE = beAmountMap.get(accCode);
                    objRE = reAmountMap.get(accCode);

                    if (glCRB != null)
                    {
                        crb.setAmount(glCRB.getAmount());
                        if (glCRB.getIsMajor() == false)
                            amountSum = glCRB.getAmount() == null ? amountSum.add(BigDecimal.ZERO) : amountSum.add(glCRB
                                    .getAmount());
                    }
                    if (pyglCRB != null)
                    {
                        crb.setPyAmount(pyglCRB.getAmount());
                        if (pyglCRB.getIsMajor() == false)
                            previousSum = pyglCRB.getAmount() == null ? previousSum.add(BigDecimal.ZERO) : previousSum
                                    .add(pyglCRB.getAmount());
                    }
                    if (objBE != null)
                    {
                        crb.setBeAmount(objBE.getAmount());
                        if (objBE.getIsMajor() == false)
                            beTotal = objBE.getAmount() == null ? beTotal.add(BigDecimal.ZERO) : beTotal.add(objBE.getAmount());
                    }
                    if (objRE != null)
                    {
                        crb.setReAmount(objRE.getAmount());
                        if (objRE.getIsMajor() == false)
                            reTotal = objRE.getAmount() == null ? reTotal.add(BigDecimal.ZERO) : reTotal.add(objRE.getAmount());
                    }
                    deptWiseWithBudgetList.add(crb);
                }
            final CommonReportBean totalCrb = new CommonReportBean("", "TOTAL", beTotal, reTotal, null, null, amountSum,
                    previousSum);
            deptWiseWithBudgetList.add(totalCrb);
            // print(deptWiseWithBudgetList);
            return deptWiseWithBudgetList;
        } else {
            for (final CommonReportBean crb : minorAndMajorCodeList)
            {
                final String accCode = crb.getAccCode();
                glCRB = minorCodeAmountMap.get(accCode);
                pyglCRB = prevousAmountMap.get(accCode);
                objBE = beAmountMap.get(accCode);
                objRE = reAmountMap.get(accCode);
                if (glCRB != null)
                {
                    crb.setAmount(glCRB.getAmount());
                    if (glCRB.getIsMajor() == false)
                        amountSum = glCRB.getAmount() == null ? amountSum.add(BigDecimal.ZERO) : amountSum.add(glCRB.getAmount());
                }
                if (pyglCRB != null)
                {
                    crb.setPyAmount(pyglCRB.getAmount());
                    if (pyglCRB.getIsMajor() == false)
                        previousSum = pyglCRB.getAmount() == null ? previousSum.add(BigDecimal.ZERO) : previousSum.add(pyglCRB
                                .getAmount());
                }
                if (objBE != null)
                {
                    crb.setBeAmount(objBE.getAmount());
                    if (objBE.getIsMajor() == false)
                        beTotal = objBE.getAmount() == null ? beTotal.add(BigDecimal.ZERO) : beTotal.add(objBE.getAmount());
                }
                if (objRE != null)
                {
                    crb.setReAmount(objRE.getAmount());
                    if (objRE.getIsMajor() == false)
                        reTotal = objRE.getAmount() == null ? reTotal.add(BigDecimal.ZERO) : reTotal.add(objRE.getAmount());
                }

            }
            final CommonReportBean totalCrb = new CommonReportBean("", "TOTAL", beTotal, reTotal, null, null, amountSum,
                    previousSum);
            minorAndMajorCodeList.add(totalCrb);
        }
        // print(minorAndMajorCodeList);
        return minorAndMajorCodeList;
            }

    private Map<String, CommonReportBean> loadIntoMap(final List<CommonReportBean> amountList, final ReportSearch reportSearch) {
        final Map<String, CommonReportBean> amountMap = new LinkedHashMap<String, CommonReportBean>();
        for (final CommonReportBean crb : amountList)
            if (reportSearch.getByDepartment() && !reportSearch.getByDetailCode())
                amountMap.put(crb.getDeptName() + "-" + crb.getAccCode(), crb);
            else
                amountMap.put(crb.getAccCode(), crb);
        return amountMap;
    }

    private void print(final List<CommonReportBean> crbList) {
        if (LOGGER.isDebugEnabled())
            for (final CommonReportBean crb : crbList)
                if (!crb.isZero())
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug(crb.toString());
    }

    private List<CommonReportBean> getBudgetApprAmountListForMinorCode(final ReportSearch reportSearch, final String isBeRe,
            final String queryStr)
    {
        Query query = null;
        if (reportSearch.getAsOnDate().getMonth() == 2 && reportSearch.getAsOnDate().getDate() == 31) {
            if (reportSearch.getByDepartment())
            {
                query = persistenceService.getSession().createSQLQuery(queryStr)
                        .addScalar("accCode", StringType.INSTANCE)
                        .addScalar("amount", BigDecimalType.INSTANCE)
                        .addScalar("isMajor", BooleanType.INSTANCE)
                        .addScalar("deptName", StringType.INSTANCE)
                        .setString("isBeRe", isBeRe)
                        .setLong("finYearId", reportSearch.getFinYearId())
                        .setLong("fundId", reportSearch.getFund().getId())
                        .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));

                if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                        && reportSearch.getFunction().getId() != -1)
                    query.setLong("functionId", reportSearch.getFunction().getId());
                if (reportSearch.getByDetailCode())
                {
                    query.setString("deptName", reportSearch.getDepartment().getName());
                    query.setString("glcode", reportSearch.getGlcode() + "%");
                } else
                    query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
            } else
            {
                query = persistenceService.getSession().createSQLQuery(queryStr)
                        .addScalar("accCode", StringType.INSTANCE)
                        .addScalar("amount", BigDecimalType.INSTANCE)
                        .addScalar("isMajor", BooleanType.INSTANCE)
                        .setString("isBeRe", isBeRe)
                        .setLong("finYearId", reportSearch.getFinYearId())
                        .setLong("fundId", reportSearch.getFund().getId())
                        .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
                if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                        && reportSearch.getFunction().getId() != -1)
                    query.setLong("functionId", reportSearch.getFunction().getId());

            }
        } else if (reportSearch.getByDepartment())
        {
            query = persistenceService.getSession().createSQLQuery(queryStr)
                    .addScalar("accCode", StringType.INSTANCE)
                    .addScalar("amount", BigDecimalType.INSTANCE)
                    .addScalar("isMajor", BooleanType.INSTANCE)
                    .addScalar("deptName", StringType.INSTANCE)
                    .setString("isBeRe", isBeRe)
                    .setDate("asOnDate", reportSearch.getAsOnDate())
                    .setLong("finYearId", reportSearch.getFinYearId())
                    .setLong("fundId", reportSearch.getFund().getId())
                    .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));

            if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                    && reportSearch.getFunction().getId() != -1)
                query.setLong("functionId", reportSearch.getFunction().getId());
            if (reportSearch.getByDetailCode())
            {
                query.setString("deptName", reportSearch.getDepartment().getName());
                query.setString("glcode", reportSearch.getGlcode() + "%");
            } else
                query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
        } else
        {
            query = persistenceService.getSession().createSQLQuery(queryStr)
                    .addScalar("accCode", StringType.INSTANCE)
                    .addScalar("amount", BigDecimalType.INSTANCE)
                    .addScalar("isMajor", BooleanType.INSTANCE)
                    .setString("isBeRe", isBeRe)
                    .setDate("asOnDate", reportSearch.getAsOnDate())
                    .setLong("finYearId", reportSearch.getFinYearId())
                    .setLong("fundId", reportSearch.getFund().getId())
                    .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
            if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                    && reportSearch.getFunction().getId() != -1)
                query.setLong("functionId", reportSearch.getFunction().getId());

        }
        return query.list();

    }

    @SuppressWarnings("unchecked")
    private List<CommonReportBean> getBudgetAmountListForMinorCode(final ReportSearch reportSearch, final String isBeRe,
            final String queryStr)
    {

        Query query = null;
        if (reportSearch.getByDepartment())
        {
            query = persistenceService.getSession().createSQLQuery(queryStr)
                    .addScalar("accCode", StringType.INSTANCE)
                    .addScalar("amount", BigDecimalType.INSTANCE)
                    .addScalar("isMajor", BooleanType.INSTANCE)
                    .addScalar("deptName", StringType.INSTANCE)
                    .setString("isBeRe", isBeRe)
                    .setDate("asOnDate", reportSearch.getAsOnDate())
                    .setLong("finYearId", reportSearch.getFinYearId())
                    .setLong("fundId", reportSearch.getFund().getId())

                    .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
            if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                    && reportSearch.getFunction().getId() != -1)
                query.setLong("functionId", reportSearch.getFunction().getId());
            if (reportSearch.getByDetailCode())
            {
                query.setString("deptName", reportSearch.getDepartment().getName());
                query.setString("glcode", reportSearch.getGlcode() + "%");
            } else
                query.setLong("FIEscheduleId", reportSearch.getFIEscheduleId());
        } else
        {
            query = persistenceService.getSession().createSQLQuery(queryStr)
                    .addScalar("accCode", StringType.INSTANCE)
                    .addScalar("amount", BigDecimalType.INSTANCE)
                    .addScalar("isMajor", BooleanType.INSTANCE)
                    .setString("isBeRe", isBeRe)
                    .setDate("asOnDate", reportSearch.getAsOnDate())
                    .setLong("finYearId", reportSearch.getFinYearId())
                    .setLong("fundId", reportSearch.getFund().getId())
                    .setResultTransformer(Transformers.aliasToBean(CommonReportBean.class));
            if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
                    && reportSearch.getFunction().getId() != -1)
                query.setLong("functionId", reportSearch.getFunction().getId());
        }

        return query.list();
    }

	private StringBuilder getBudgetQueryForMinorCodes(final ReportSearch reportSearch) {

		final StringBuilder queryStr = new StringBuilder();
		queryStr.append(String.format(" select SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
				.append(") as accCode, sum(bd.approvedamount) as amount ,0 as isMajor ");
		if (reportSearch.getByDepartment())
			queryStr.append(",d.dept_name  as deptName ");
		queryStr.append(
				" from egf_budgetdetail bd , egf_budgetgroup bg,egf_budget b, chartofaccounts coa, eg_wf_states wfs");
		if (reportSearch.getByDetailCode())
			queryStr.append(",eg_department d");
		else if (reportSearch.getByDepartment())
			queryStr.append(",eg_department d, chartofaccounts minorcoa");
		queryStr.append(
				" where ((bg.maxcode<=coa.id and bg.mincode>=coa.id) or bg.majorcode=coa.id ) and bd.budgetgroup= bg.id  ")
				.append(" and bd.state_id=wfs.id and wfs.created_date<=:asOnDate and wfs.value='END' ");
		queryStr.append(
				" and bd.budget=b.id and b.isbere=:isBeRe and b.financialyearid=:finYearId and bd.fund=:fundId ");
		if (reportSearch.getByDetailCode())
			queryStr.append(
					" and d.id_dept=bd.executing_department and d.dept_name=:deptName and coa.glcode like :glcode ");
		else if (reportSearch.getByDepartment())
			queryStr.append(" and d.id_dept=bd.executing_department  and minorcoa.FIEscheduleId=:FIEscheduleId")
					.append(String.format(" and  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
					.append(")=minorcoa.glcode ");
		if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
				&& reportSearch.getFunction().getId() != -1)
			queryStr.append("  and bd.function=:functionId ");
		if (reportSearch.getIncExp().equals("E"))
			queryStr.append(" and (coa.type='E'").append(capExpCodeCond)
					.append(String.format(") group by SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
					.append(")");
		else
			queryStr.append(String.format(" and (coa.type='I') group by SUBSTR(coa.glcode,1,%d",
					reportSearch.getMinorCodeLen())).append(")");
		if (reportSearch.getByDepartment())
			queryStr.append(" ,d.dept_name ");
		if (!reportSearch.getByDepartment()) {
			queryStr.append(" UNION ");
			queryStr.append(" select coa.majorCode as accCode, sum(bd.approvedamount) as amount,1 as isMajor ");
			if (reportSearch.getByDepartment())
				queryStr.append(",d.dept_name  as deptName ");
			queryStr.append(
					" from egf_budgetdetail bd , egf_budgetgroup bg,egf_budget b, chartofaccounts coa, eg_wf_states wfs ");
			if (reportSearch.getByDepartment())
				queryStr.append(",eg_department d");

			queryStr.append(
					"where ((bg.maxcode<=coa.id and bg.mincode>=coa.id) or bg.majorcode=coa.id ) and bd.budgetgroup= bg.id ")
					.append(" and bd.budget=b.id and  bd.state_id=wfs.id and wfs.created_date<=:asOnDate and wfs.value='END'")
					.append(" and b.isbere=:isBeRe and b.financialyearid=:finYearId and bd.fund=:fundId  ");
			if (reportSearch.getByDepartment())
				queryStr.append(" and d.id_dept=bd.executing_department and coa.FIEscheduleId=:FIEscheduleId ");
			if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
					&& reportSearch.getFunction().getId() != -1)
				queryStr.append("  and bd.function=:functionId ");
			if (reportSearch.getIncExp().equals("E"))
				queryStr.append(" and (coa.type='E'").append(capExpCodeCond)
						.append(") and coa.majorcode is not null  group by coa.majorCode ");
			else
				queryStr.append(" and (coa.type='I') and coa.majorcode is not null  group by coa.majorCode ");
			if (reportSearch.getByDepartment())
				queryStr.append(" d.dept_name");
		}
		queryStr.append(" order by 3,1");
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("query is " + queryStr.toString());
		return queryStr;
	}

	private StringBuilder getBudgetReappQueryForMinorCodes(final ReportSearch reportSearch) {
		final StringBuilder queryStr = new StringBuilder();
		queryStr.append(String.format(" select SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
				.append(") as accCode, sum(bdr.addition_amount- bdr.deduction_amount) as amount ,0 as isMajor ");
		if (reportSearch.getByDepartment())
			queryStr.append(",d.dept_name  as deptName ");
		queryStr.append(" from egf_budgetdetail bd , egf_budgetgroup bg,egf_budget b, chartofaccounts coa,")
				.append(" egf_budget_reappropriation bdr,eg_wf_states wfs");
		if (reportSearch.getByDetailCode())
			queryStr.append(",eg_department d");
		else if (reportSearch.getByDepartment())
			queryStr.append(",eg_department d,chartofaccounts minorcoa ");
		queryStr.append(" where ((bg.maxcode<=coa.id and bg.mincode>=coa.id) or bg.majorcode=coa.id )")
				.append(" and bd.budgetgroup= bg.id  and bdr.budgetdetail=bd.id")
				.append(" and bdr.state_id=wfs.id and wfs.value='END' ");
		if (!(reportSearch.getAsOnDate().getMonth() == 2 && reportSearch.getAsOnDate().getDate() == 31))
			queryStr.append(" and wfs.created_date<=:asOnDate ");
		queryStr.append(
				" and bd.budget=b.id and b.isbere=:isBeRe and b.financialyearid=:finYearId and bd.fund=:fundId ");
		if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
				&& reportSearch.getFunction().getId() != -1)
			queryStr.append("  and bd.function=:functionId ");
		if (reportSearch.getByDetailCode())
			queryStr.append(
					" and d.id_dept=bd.executing_department and d.dept_name=:deptName and coa.glcode like :glcode   ");
		else if (reportSearch.getByDepartment())
			queryStr.append(" and d.id_dept=bd.executing_department and minorcoa.FIEscheduleId=:FIEscheduleId")
					.append(String.format(" and  SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen()))
					.append(")=minorcoa.glcode ");
		queryStr.append(" and (coa.type='E'").append(capExpCodeCond)
				.append(String.format(") group by SUBSTR(coa.glcode,1,%d", reportSearch.getMinorCodeLen())).append(")");
		if (reportSearch.getByDepartment())
			queryStr.append(" ,d.dept_name ");
		if (!reportSearch.getByDepartment()) {
			queryStr.append(" UNION ")
					.append(String.format(" select SUBSTR(coa.glcode,1,%d", reportSearch.getMajorCodeLen()))
					.append(") as accCode, sum(bdr.addition_amount- bdr.deduction_amount) as amount,1 as isMajor ");
			if (reportSearch.getByDepartment())
				queryStr.append(",bd.executing_derpartment  as deptName ");
			queryStr.append(" from egf_budgetdetail bd , egf_budgetgroup bg,egf_budget b, chartofaccounts coa,").append(
					"eg_wf_states wfs,egf_budget_reappropriation bdr where ((bg.maxcode<=coa.id and bg.mincode>=coa.id)")
					.append(" or bg.majorcode=coa.id ) and bd.budgetgroup= bg.id ")
					.append("  and bdr.budgetdetail=bd.id and bd.budget=b.id and bdr.state_id=wfs.id  and wfs.value='END'")
					.append(" and b.isbere=:isBeRe and b.financialyearid=:finYearId and bd.fund=:fundId ");
			if (!(reportSearch.getAsOnDate().getMonth() == 2 && reportSearch.getAsOnDate().getDate() == 31))
				queryStr.append(" and wfs.created_date<=:asOnDate ");
			if (reportSearch.getFunction() != null && reportSearch.getFunction().getId() != null
					&& reportSearch.getFunction().getId() != -1)
				queryStr.append("  and bd.function=:functionId ");
			queryStr.append(" and (coa.type='E'").append(capExpCodeCond)
					.append(String.format(") group by SUBSTR(coa.glcode,1,%d", reportSearch.getMajorCodeLen()))
					.append(")");
			if (reportSearch.getByDepartment())
				queryStr.append(" bd.executing_derpartment ");
		}
		queryStr.append(" order by 1 desc");
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("query is " + queryStr.toString());
		return queryStr;
	}

    public void setReportSearch(final ReportSearch reportSearch) {
    }

    public BigDecimal round(final BigDecimal value) {
        final BigDecimal val = value.setScale(2, BigDecimal.ROUND_HALF_UP);
        return val;
    }
}