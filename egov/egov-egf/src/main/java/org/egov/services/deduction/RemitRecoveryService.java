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
/**
 *
 */
package org.egov.services.deduction;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.egov.commons.CVoucherHeader;
import org.egov.commons.utils.EntityType;
import org.egov.dao.recoveries.TdsHibernateDAO;
import org.egov.dao.voucher.VoucherHibernateDAO;
import org.egov.egf.model.AutoRemittanceBeanReport;
import org.egov.infra.validation.exception.ValidationError;
import org.egov.infra.validation.exception.ValidationException;
import org.egov.infstr.services.PersistenceService;
import org.egov.model.deduction.RemittanceBean;
import org.egov.model.recoveries.Recovery;
import org.egov.utils.Constants;
import org.egov.utils.VoucherHelper;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author manoranjan
 *
 */
public class RemitRecoveryService {

    private PersistenceService persistenceService;

    private static final Logger LOGGER = Logger.getLogger(RemitRecoveryService.class);
    private static final SimpleDateFormat DDMMYYYY = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private VoucherHibernateDAO voucherHibDAO;
    @Autowired
    private TdsHibernateDAO tdsHibernateDAO;

	public List<RemittanceBean> getPendingRecoveryDetails(final RemittanceBean remittanceBean,
			final CVoucherHeader voucherHeader, final Integer detailKeyId) throws ValidationException {
		final List<RemittanceBean> listRemitBean = new ArrayList<>();
		final StringBuilder query = new StringBuilder();
		final Map<String, Object> params = new HashMap<>();
		query.append(
				"select vh.name,vh.voucherNumber ,vh.voucherDate,egr.gldtlamt,gld.detailTypeId.id,gld.detailKeyId,egr.id ")
				.append(" from CVoucherHeader vh ,Vouchermis mis , CGeneralLedger gl ,CGeneralLedgerDetail gld , EgRemittanceGldtl egr ,")
				.append(" Recovery rec  where ")
				.append("  rec.chartofaccounts.id = gl.glcodeId.id and gld.id = egr.generalledgerdetail.id")
				.append(" and  gl.id = gld.generalLedgerId.id and vh.id = gl.voucherHeaderId.id ")
				.append(" and mis.voucherheaderid.id = vh.id  and vh.status=0  and vh.fundId.id=:vhFundId  and  egr.gldtlamt - ")
				.append(" (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end")
				.append("  from EgRemittanceGldtl egr1,")
				.append("EgRemittanceDetail egd,EgRemittance  eg,CVoucherHeader vh  where vh.status not in (1,2,4)")
				.append(" and  eg.voucherheader.id=vh.id")
				.append(" and egd.egRemittance.id=eg.id and egr1.id=egd.egRemittanceGldtl.id and egr1.id=egr.id) != 0 and rec.id =:recId")
				.append(" and ( egr.recovery.id = :recId").append(" OR egr.recovery.id is null )")
				.append(" and vh.voucherDate <=:vhVoucherDate");
		params.put("vhFundId", voucherHeader.getFundId().getId());
		params.put("recId", remittanceBean.getRecoveryId());
		params.put("vhVoucherDate", Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()));
		if (detailKeyId != null && detailKeyId != -1) {
			query.append(" and egr.generalledgerdetail.detailkeyid = :gldDetailKeyId");
			params.put("gldDetailKeyId", detailKeyId);
		}
		Entry<String, Map<String, Object>> queryWithParams = VoucherHelper.getMisQuery(voucherHeader).entrySet()
				.iterator().next();
		query.append(queryWithParams.getKey()).append(" order by vh.voucherNumber,vh.voucherDate");
		params.putAll(queryWithParams.getValue());
		populateDetails(voucherHeader, listRemitBean, query.toString(), params);
		return listRemitBean;
	}

    public List<RemittanceBean> getRecoveryDetails(final RemittanceBean remittanceBean, final CVoucherHeader voucherHeader)
            throws ValidationException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | Start");
        final List<RemittanceBean> listRemitBean = new ArrayList<>();
        final StringBuilder dateQry = new StringBuilder();
        StringBuilder query = new StringBuilder();
        if (remittanceBean.getFromVhDate() != null && voucherHeader.getVoucherDate() != null)
            dateQry.append(" and vh.VOUCHERDATE >='" + Constants.DDMMYYYYFORMAT1.format(remittanceBean.getFromVhDate())
                    + "' and vh.VOUCHERDATE <='" + Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()) + "' ");
        else
            dateQry.append(" and vh.VOUCHERDATE <='" + Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()) + "' ");
        if (remittanceBean.getBank() != null && remittanceBean.getBankBranchId() != null
                && remittanceBean.getBankAccountId() != null) {
            query = getRecoveryListForSelectedBank(remittanceBean, voucherHeader, dateQry);
        } else {
            query.append(" SELECT vh.NAME  AS col_0_0_,  vh.VOUCHERNUMBER AS col_1_0_,  vh.VOUCHERDATE   AS col_2_0_,");
            query.append(" egr.GLDTLAMT   AS col_3_0_,  gld.DETAILTYPEID  AS col_4_0_,  gld.DETAILKEYID   AS col_5_0_,");
            query.append(
                    " egr.ID    AS col_6_0_, (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end");
            query.append(" from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
            query.append(
                    " where vh.status!=4 and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid ");
            query.append(" and egr1.id=egr.id) As col_7_0 , mis.departmentcode as col_8_0,mis.functionid as col_9_0");
            query.append(
                    "  FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  GENERALLEDGERDETAIL gld,  EG_REMITTANCE_GLDTL egr,  TDS recovery5_");
            query.append(
                    " WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gld.ID =egr.GLDTLID AND gl.ID =gld.GENERALLEDGERID AND vh.ID =gl.VOUCHERHEADERID");
            query.append(" AND mis.VOUCHERHEADERID  =vh.ID AND vh.STATUS    =0 AND vh.FUNDID    =");
            query.append(voucherHeader.getFundId().getId());
            query.append(" AND egr.GLDTLAMT-");
            query.append(
                    " (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
            query.append(
                    " where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid and egr1.id=egr.id)");
            query.append(" <>0 AND recovery5_.ID  = ");
            query.append(remittanceBean.getRecoveryId()).append(" AND (egr.TDSID = ");
            query.append(remittanceBean.getRecoveryId());
            query.append(" OR egr.TDSID  IS NULL) ");
            query.append(dateQry);
            query.append(getMisSQlQuery(voucherHeader));
            query.append(" ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | query := " + query.toString());

        populateDetailsBySQL(voucherHeader, listRemitBean, query);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | listRemitBean size : " + listRemitBean.size());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | End");
        return listRemitBean;
    }
    
    public boolean isNonControlledCodeTds(RemittanceBean remittanceBean){
        Recovery recovery = tdsHibernateDAO.findById(remittanceBean.getRecoveryId(), false);
        final String query = "from CChartOfAccountDetail where glCodeId.id="+recovery.getChartofaccounts().getId();
        Query pst = persistenceService.getSession().createQuery(query);
        return pst.list().isEmpty();
    }
    
    public List<RemittanceBean> getRecoveryDetailsForNonControlledCode(final RemittanceBean remittanceBean, final CVoucherHeader voucherHeader){
            final List<RemittanceBean> listRemitBean = new ArrayList<>();
            StringBuilder query2 = new StringBuilder();
            final StringBuilder dateQry = new StringBuilder();
            if (remittanceBean.getFromVhDate() != null && voucherHeader.getVoucherDate() != null)
                dateQry.append(" and vh.VOUCHERDATE >='" + Constants.DDMMYYYYFORMAT1.format(remittanceBean.getFromVhDate())
                        + "' and vh.VOUCHERDATE <='" + Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()) + "' ");
            else
                dateQry.append(" and vh.VOUCHERDATE <='" + Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()) + "' ");
            query2.append("SELECT vh.NAME,  vh.VOUCHERNUMBER,  vh.VOUCHERDATE, egr.glamt, egr.ID, ");
            query2.append("(select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh where vh.status!=4 and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egd.REMITTANCEGLID=egr1.id  and egr1.id=egr.id) As col_7_0 , ");
            query2.append("mis.departmentcode,mis.functionid  ");
            query2.append("FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  EG_REMITTANCE_GL egr,  TDS recovery5_ ");
            query2.append("WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gl.id=egr.glid and ");
            query2.append("vh.ID =gl.VOUCHERHEADERID AND mis.VOUCHERHEADERID  =vh.ID AND ");
            query2.append("vh.STATUS =0 AND vh.FUNDID =");
            query2.append(voucherHeader.getFundId().getId());
            query2.append(" AND egr.glamt- (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egd.REMITTANCEGLID=egr1.id and egr1.id=egr.id) <>0 AND ");
            query2.append("recovery5_.ID  = ");
            query2.append(remittanceBean.getRecoveryId());
            query2.append(" AND (egr.TDSID = ");
            query2.append(remittanceBean.getRecoveryId());
            query2.append(" OR egr.TDSID  IS NULL) ");
            query2.append(dateQry);
            query2.append(getMisSQlQuery(voucherHeader));
            query2.append(" ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");
            populateNonConrolledTdsDataBySQL(voucherHeader, listRemitBean, query2);
            return listRemitBean;
    }
    
    public List<RemittanceBean> getRecoveryDetailsForNonControlledCode(final String selectedRows){
        final List<RemittanceBean> listRemitBean = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT vh.NAME,  vh.VOUCHERNUMBER,  vh.VOUCHERDATE, egr.glamt, egr.ID, ");
        query.append("(select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh where vh.status!=4 and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egd.REMITTANCEGLID=egr1.id and egr1.id=egr.id) As col_7_0 , ");
        query.append("mis.departmentcode,mis.functionid  ");
        query.append("FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  EG_REMITTANCE_GL egr,  TDS recovery5_ ");
        query.append("WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gl.id=egr.glid and ");
        query.append("vh.ID =gl.VOUCHERHEADERID AND mis.VOUCHERHEADERID  =vh.ID AND ");
        query.append("vh.STATUS =0 AND egr.id in ( ");
        query.append(selectedRows);
        query.append(" ) and recovery5_.isactive=true");
        query.append(" AND egr.glamt- (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egd.REMITTANCEGLID=egr1.id and egr1.id=egr.id) <>0 ");
        query.append("ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");
        populateNonConrolledTdsDataBySQL(null, listRemitBean, query);
        return listRemitBean;
}

    public StringBuilder getRecoveryListForSelectedBank(final RemittanceBean remittanceBean, final CVoucherHeader voucherHeader,
            final StringBuilder dateQuery) {
        StringBuilder query = new StringBuilder();
        query.append(" SELECT vh.NAME  AS col_0_0_,  vh.VOUCHERNUMBER AS col_1_0_,  vh.VOUCHERDATE   AS col_2_0_,");
        query.append(" egr.GLDTLAMT   AS col_3_0_,  gld.DETAILTYPEID  AS col_4_0_,  gld.DETAILKEYID   AS col_5_0_,");
        query.append(
                " egr.ID    AS col_6_0_, (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end");
        query.append(" from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status not in (4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid ");
        query.append(" and egr1.id=egr.id) As col_7_0 , mis.departmentcode as col_8_0,mis.functionid as col_9_0");
        query.append(
                "  FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  GENERALLEDGERDETAIL gld,  EG_REMITTANCE_GLDTL egr,  TDS recovery5_ ,PAYMENTHEADER ph,miscbilldetail misbill");
        query.append(
                " WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gld.ID =egr.GLDTLID AND gl.ID =gld.GENERALLEDGERID AND vh.ID =gl.VOUCHERHEADERID");
        query.append(
                " AND mis.VOUCHERHEADERID  =vh.ID AND vh.STATUS    =0 and misbill.billvhid=vh.id and misbill.payvhid=ph.voucherheaderid and (select status from voucherheader where id=misbill.payvhid )=0  AND ph.bankaccountnumberid=");
        query.append(remittanceBean.getBankAccountId()).append(" and vh.FUNDID    =");
        query.append(voucherHeader.getFundId().getId());
        query.append(" AND egr.GLDTLAMT-");
        query.append(
                " (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid and egr1.id=egr.id)");
        query.append(" <>0 AND recovery5_.ID  = ");
        query.append(remittanceBean.getRecoveryId()).append(" AND (egr.TDSID = ");
        query.append(remittanceBean.getRecoveryId());
        query.append(" OR egr.TDSID  IS NULL) ");
        query.append(dateQuery);
        query.append(getMisSQlQuery(voucherHeader));
        query.append(" ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");

        return query;
    }

    public List<RemittanceBean> getRecoveryDetails(final String selectedRows)
            throws ValidationException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | Start");
        final List<RemittanceBean> listRemitBean = new ArrayList<RemittanceBean>();

        StringBuilder query = new StringBuilder();
        query.append(" SELECT vh.NAME  AS col_0_0_,  vh.VOUCHERNUMBER AS col_1_0_,  vh.VOUCHERDATE   AS col_2_0_,");
        query.append(" egr.GLDTLAMT   AS col_3_0_,  gld.DETAILTYPEID  AS col_4_0_,  gld.DETAILKEYID   AS col_5_0_,");
        query.append(
                " egr.ID    AS col_6_0_, (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end");
        query.append(" from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status!=4 and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid ");
        query.append(" and egr1.id=egr.id) As col_7_0 , mis.departmentcode as col_8_0,mis.functionid as col_9_0");
        query.append(
                "  FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  GENERALLEDGERDETAIL gld,  EG_REMITTANCE_GLDTL egr,  TDS recovery5_");
        query.append(
                " WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gld.ID =egr.GLDTLID AND gl.ID =gld.GENERALLEDGERID AND vh.ID =gl.VOUCHERHEADERID");
        query.append(" AND mis.VOUCHERHEADERID  =vh.ID AND vh.STATUS    =0 ");
        query.append(" and egr.id in ( ");
        query.append(selectedRows);
        query.append(" ) and recovery5_.isactive=true");
        query.append(" AND egr.GLDTLAMT-");
        query.append(
                " (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid and egr1.id=egr.id)");
        query.append(" <>0  ");
        query.append(" ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | query := " + query.toString());

        populateDetailsBySQL(null, listRemitBean, query);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | listRemitBean size : " + listRemitBean.size());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | End");
        return listRemitBean;
    }

    public List<RemittanceBean> getRecoveryDetailsForReport(final RemittanceBean remittanceBean,
            final CVoucherHeader voucherHeader,
            final Integer detailKeyId) throws ValidationException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | Start");
        final List<RemittanceBean> listRemitBean = new ArrayList<RemittanceBean>();
        StringBuilder query = new StringBuilder();
        query.append(" SELECT vh.NAME     AS col_0_0_,  vh.VOUCHERNUMBER AS col_1_0_,  vh.VOUCHERDATE   AS col_2_0_,");
        query.append(" egr.GLDTLAMT      AS col_3_0_,  gld.DETAILTYPEID  AS col_4_0_,  gld.DETAILKEYID   AS col_5_0_,");
        query.append(
                " egr.ID            AS col_6_0_, (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end");
        query.append(" from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status!=4 and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid ");
        query.append(" and egr1.id=egr.id) As col_7_0, mis.departmentcode as col_8_0,mis.functionid as col_9_0");
        query.append(
                "  FROM VOUCHERHEADER vh,  VOUCHERMIS mis,  GENERALLEDGER gl,  GENERALLEDGERDETAIL gld,  EG_REMITTANCE_GLDTL egr,  TDS recovery5_");
        query.append(
                " WHERE recovery5_.GLCODEID  =gl.GLCODEID AND gld.ID =egr.GLDTLID AND gl.ID =gld.GENERALLEDGERID AND vh.ID =gl.VOUCHERHEADERID");
        query.append(" AND mis.VOUCHERHEADERID  =vh.ID AND vh.STATUS    =0 AND vh.FUNDID    =");
        query.append(voucherHeader.getFundId().getId());
        query.append(" AND egr.GLDTLAMT-");
        query.append(
                " (select  case when sum(egd.remittedamt) is null then 0 else sum(egd.remittedamt) end from EG_REMITTANCE_GLDTL egr1,eg_remittance_detail egd,eg_remittance  eg,voucherheader vh");
        query.append(
                " where vh.status not in (1,2,4) and  eg.PAYMENTVHID=vh.id and egd.remittanceid=eg.id and egr1.id=egd.remittancegldtlid and egr1.id=egr.id)");
        query.append(" <>0 AND recovery5_.ID  = ");
        query.append(remittanceBean.getRecoveryId()).append(" AND (egr.TDSID  = ");
        query.append(remittanceBean.getRecoveryId());
        query.append(" OR egr.TDSID  IS NULL) AND vh.VOUCHERDATE <= '");
        query.append(Constants.DDMMYYYYFORMAT1.format(voucherHeader.getVoucherDate()) + "' ");
        if (remittanceBean.getFromDate() != null && !remittanceBean.getFromDate().isEmpty())
            query.append("  and vh.VoucherDate>= '")
                    .append(remittanceBean.getFromDate() + "'");
        if (detailKeyId != null && detailKeyId.intValue() != 0)
            query.append(" and gld.detailkeyid=" + detailKeyId);
        query.append("   " + getMisSQlQuery(voucherHeader))
                .append(" ORDER BY vh.VOUCHERNUMBER,  vh.VOUCHERDATE");

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | query := " + query.toString());

        populateDetailsBySQL(voucherHeader, listRemitBean, query);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | listRemitBean size : " + listRemitBean.size());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("RemitRecoveryService | getRecoveryDetails | End");
        return listRemitBean;
    }

    /**
     * @param voucherHeader
     * @return
     */
    private Object getMisSQlQuery(final CVoucherHeader voucherHeader) {
        final StringBuilder misQuery = new StringBuilder();
        if (null != voucherHeader && null != voucherHeader.getVouchermis()) {
            if (null != voucherHeader.getVouchermis().getDepartmentcode()
                    && !voucherHeader.getVouchermis().getDepartmentcode().equalsIgnoreCase("-1")) {
                misQuery.append("and  mis.departmentcode='");
                misQuery.append(voucherHeader.getVouchermis().getDepartmentcode() + "'");
            }
            if (null != voucherHeader.getVouchermis().getFunctionary()
                    && null != voucherHeader.getVouchermis().getFunctionary().getId()
                    && -1 != voucherHeader.getVouchermis().getFunctionary().getId()) {
                misQuery.append(" and mis.functionaryid=");
                misQuery.append(voucherHeader.getVouchermis().getFunctionary().getId());
            }
            if (null != voucherHeader.getVouchermis().getFunction()
                    && null != voucherHeader.getVouchermis().getFunction().getId()
                    && -1 != voucherHeader.getVouchermis().getFunction().getId()) {
                misQuery.append(" and mis.functionid=");
                misQuery.append(voucherHeader.getVouchermis().getFunction().getId());
            }
            if (null != voucherHeader.getVouchermis().getSchemeid()
                    && null != voucherHeader.getVouchermis().getSchemeid().getId()
                    && -1 != voucherHeader.getVouchermis().getSchemeid().getId()) {
                misQuery.append(" and mis.schemeid=");
                misQuery.append(voucherHeader.getVouchermis().getSchemeid().getId());
            }
            if (null != voucherHeader.getVouchermis().getSubschemeid()
                    && null != voucherHeader.getVouchermis().getSubschemeid().getId()
                    && -1 != voucherHeader.getVouchermis().getSubschemeid().getId()) {
                misQuery.append(" and mis.subschemeid=");
                misQuery.append(voucherHeader.getVouchermis().getSubschemeid().getId());
            }
            if (null != voucherHeader.getVouchermis().getFundsource()
                    && null != voucherHeader.getVouchermis().getFundsource().getId()
                    && -1 != voucherHeader.getVouchermis().getFundsource().getId()) {
                misQuery.append(" and mis.fundsourceid=");
                misQuery.append(voucherHeader.getVouchermis().getFundsource().getId());
            }
            if (null != voucherHeader.getVouchermis().getDivisionid()
                    && null != voucherHeader.getVouchermis().getDivisionid().getId()
                    && -1 != voucherHeader.getVouchermis().getDivisionid().getId()) {
                misQuery.append(" and mis.divisionid=");
                misQuery.append(voucherHeader.getVouchermis().getDivisionid().getId());
            }
        }
        return misQuery.toString();
    }

    private void populateDetailsBySQL(final CVoucherHeader voucherHeader, final List<RemittanceBean> listRemitBean,
            final StringBuilder query) {
        RemittanceBean remitBean;
        final SQLQuery searchSQLQuery = persistenceService.getSession().createSQLQuery(query.toString());
        final List<Object[]> list = searchSQLQuery.list();
        for (final Object[] element : list) {
            remitBean = new RemittanceBean();
            remitBean.setVoucherName(element[0].toString());
            remitBean.setVoucherNumber(element[1].toString());
            try {
                remitBean.setVoucherDate(DDMMYYYY.format(YYYYMMDD.parse(element[2].toString())));
            } catch (final ParseException e) {
                LOGGER.error("Exception Occured while Parsing instrument date" + e.getMessage());
            }
            remitBean.setDeductionAmount(BigDecimal.valueOf(Double.parseDouble(element[3].toString())));
            if (element[7] != null)
                remitBean.setEarlierPayment(BigDecimal.valueOf(Double.parseDouble(element[7].toString())));
            else
                remitBean.setEarlierPayment(BigDecimal.ZERO);
            if (remitBean.getEarlierPayment() != null && remitBean.getEarlierPayment().compareTo(BigDecimal.ZERO) != 0)
                remitBean.setAmount(remitBean.getDeductionAmount().subtract(remitBean.getEarlierPayment()));
            else
                remitBean.setAmount(remitBean.getDeductionAmount());
            remitBean.setDepartmentId(element[8].toString());
            if(element[9]!=null)
                remitBean.setFunctionId(Long.valueOf(element[9].toString()));
            final EntityType entity = voucherHibDAO.getEntityInfo(Integer.valueOf(element[5].toString()),
                    Integer.valueOf(element[4].toString()));
            if (entity == null) {
                LOGGER.error("Entity Might have been deleted........................");
                LOGGER.error("The detail key " + Integer.valueOf(element[5].toString()) + " of detail type "
                        + Integer.valueOf(element[4].toString())
                        + "Missing in voucher" + remitBean.getVoucherNumber());
                throw new ValidationException(Arrays.asList(new ValidationError("Entity information not available for voucher "
                        + remitBean.getVoucherNumber(), "Entity information not available for voucher "
                                + remitBean.getVoucherNumber())));
            }
            // Exception here
            if (voucherHeader == null){
                if (remitBean.getEarlierPayment() != null && remitBean.getEarlierPayment().compareTo(BigDecimal.ZERO) != 0)
                    remitBean.setPartialAmount(remitBean.getDeductionAmount().subtract(remitBean.getEarlierPayment()));
                else{
                    remitBean.setPartialAmount(remitBean.getDeductionAmount());
                }
            }
            remitBean.setPartyCode(entity.getCode());
            remitBean.setPartyName(entity.getName());
            remitBean.setPanNo(entity.getPanno());
            remitBean.setDetailTypeId(Integer.valueOf(element[4].toString()));
            remitBean.setDetailKeyid(Integer.valueOf(element[5].toString()));
            remitBean.setRemittance_gl_dtlId(Integer.valueOf(element[6].toString()));
            listRemitBean.add(remitBean);
        }
    }
    
    private void populateNonConrolledTdsDataBySQL(final CVoucherHeader voucherHeader, final List<RemittanceBean> listRemitBean,
            final StringBuilder query) {
        RemittanceBean remitBean;
        final SQLQuery searchSQLQuery = persistenceService.getSession().createSQLQuery(query.toString());
        final List<Object[]> list = searchSQLQuery.list();
        for (final Object[] element : list) {
            remitBean = new RemittanceBean();
            remitBean.setVoucherName(element[0].toString());
            remitBean.setVoucherNumber(element[1].toString());
            try {
                remitBean.setVoucherDate(DDMMYYYY.format(YYYYMMDD.parse(element[2].toString())));
            } catch (final ParseException e) {
                LOGGER.error("Exception Occured while Parsing instrument date" + e.getMessage());
            }
            remitBean.setDeductionAmount(BigDecimal.valueOf(Double.parseDouble(element[3].toString())));
            if (element[5] != null)
                remitBean.setEarlierPayment(BigDecimal.valueOf(Double.parseDouble(element[5].toString())));
            else
                remitBean.setEarlierPayment(BigDecimal.ZERO);
            if (remitBean.getEarlierPayment() != null && remitBean.getEarlierPayment().compareTo(BigDecimal.ZERO) != 0)
                remitBean.setAmount(remitBean.getDeductionAmount().subtract(remitBean.getEarlierPayment()));
            else
                remitBean.setAmount(remitBean.getDeductionAmount());
            remitBean.setDepartmentId(element[6].toString());
            if(element[7]!=null)
                remitBean.setFunctionId(Long.valueOf(element[7].toString()));
            if (voucherHeader == null){
                if (remitBean.getEarlierPayment() != null && remitBean.getEarlierPayment().compareTo(BigDecimal.ZERO) != 0)
                    remitBean.setPartialAmount(remitBean.getDeductionAmount().subtract(remitBean.getEarlierPayment()));
                else{
                    remitBean.setPartialAmount(remitBean.getDeductionAmount());
                }
            }
            remitBean.setRemittance_gl_Id(Integer.valueOf(element[4].toString()));
            listRemitBean.add(remitBean);
        }
    }

    private void populateDetails(final CVoucherHeader voucherHeader, final List<RemittanceBean> listRemitBean,
            final String query, Map<String, Object> params) {
        RemittanceBean remitBean;
        final Query qry = persistenceService.getSession().createQuery(query);
        params.entrySet().forEach(entry -> qry.setParameter(entry.getKey(), entry.getValue()));
        final List<Object[]> list = qry.list();
        for (final Object[] element : list) {
            remitBean = new RemittanceBean();
            remitBean.setVoucherName(element[0].toString());
            remitBean.setVoucherNumber(element[1].toString());
            try {
                remitBean.setVoucherDate(DDMMYYYY.format(YYYYMMDD.parse(element[2].toString())));
            } catch (final ParseException e) {
                LOGGER.error("Exception Occured while Parsing instrument date" + e.getMessage());
            }
            remitBean.setAmount(BigDecimal.valueOf(Double.parseDouble(element[3].toString())));
            final EntityType entity = voucherHibDAO.getEntityInfo(Integer.valueOf(element[5].toString()),
                    Integer.valueOf(element[4].toString()));
            remitBean.setPartyCode(entity.getCode());
            remitBean.setPartyName(entity.getName());
            remitBean.setPanNo(entity.getPanno());
            remitBean.setDetailTypeId(Integer.valueOf(element[4].toString()));
            remitBean.setDetailKeyid(Integer.valueOf(element[5].toString()));
            remitBean.setRemittance_gl_dtlId(Integer.valueOf(element[6].toString()));
            listRemitBean.add(remitBean);
        }
    }

    public void setPersistenceService(final PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void setVoucherHibDAO(final VoucherHibernateDAO voucherHibDAO) {
        this.voucherHibDAO = voucherHibDAO;
    }

    public List<AutoRemittanceBeanReport> populateAutoRemittanceDetailbySQL(final Query sqlQuery) {
        final List<AutoRemittanceBeanReport> remittanceList = sqlQuery.list();
        final List<AutoRemittanceBeanReport> autoRemittance = new ArrayList<AutoRemittanceBeanReport>(0);

        final StringBuffer voucherQueryOne = new StringBuffer(
                "SELECT   remgldtl.REMITTEDAMT AS remittedAmount,"
                        +
                        "( SELECT SUM(creditamount) FROM GENERALLEDGER gld1 WHERE gld1.voucherheaderid =gld.voucherheaderid) AS billAmount,"
                        +
                        " vh.VOUCHERNUMBER AS voucherNumber, miscbilldtl.billnumber AS billNumber,remdtl.id as remittanceDTId,"
                        +
                        " gldtl.DETAILTYPEID  as detailKeyTypeId ,  gldtl.DETAILKEYID as detailKeyId,vh.id as voucherId,billmis.BILLID as billId"
                        +
                        " FROM EG_REMITTANCE_DETAIL remdtl,EG_REMITTANCE_GLDTL remgldtl, "
                        +
                        " GENERALLEDGERDETAIL gldtl,GENERALLEDGER gld,VOUCHERHEADER vh, MISCBILLDETAIL miscbilldtl,eg_billregistermis billmis "
                        +
                        " WHERE  remdtl.REMITTANCEGLDTLID = remgldtl.id" +
                        " AND gldtl.ID = remgldtl.GLDTLID " +
                        " AND gldtl.GENERALLEDGERID = gld.id" +
                        " AND gld.VOUCHERHEADERID =vh.id" +
                        " AND miscbilldtl.billvhid =vh.id" +
                        " AND billmis.VOUCHERHEADERID=vh.id ");
        StringBuffer inquery = new StringBuffer(" AND remdtl.id in ( ");
        int i = 1;
        if (null != remittanceList && !remittanceList.isEmpty()) {

            for (final AutoRemittanceBeanReport remittance : remittanceList) {
                if (i % 1000 == 0) {
                    inquery.append(")");
                    final StringBuffer voucherQueryTwo = new StringBuffer(
                            voucherQueryOne
                                    + inquery.toString()
                                    +
                                    " GROUP BY  vh.vouchernumber, miscbilldtl.billnumber , remgldtl.remittedamt, remdtl.ID,  gldtl.detailtypeid , gldtl.detailkeyid,vh.id,gld.voucherheaderid,billmis.BILLID");
                    final Query sqlVoucherQuery = persistenceService.getSession().createSQLQuery(voucherQueryTwo.toString())
                            .addScalar("remittedAmount").addScalar("billAmount").addScalar("voucherNumber")
                            .addScalar("billNumber").addScalar("remittanceDTId")
                            .addScalar("detailKeyTypeId").addScalar("detailKeyId").addScalar("voucherId").addScalar("billId")
                            .setResultTransformer(Transformers.aliasToBean(AutoRemittanceBeanReport.class));
                    autoRemittance.addAll(sqlVoucherQuery.list());
                    inquery = new StringBuffer(" AND remdtl.id in ( " + remittance.getRemittanceDTId().toString());
                } else {
                    if (i != 1)
                        inquery.append(",");
                    inquery.append(remittance.getRemittanceDTId().toString());
                }
                i++;
            }
            inquery.append(")");
            final StringBuffer voucherQueryTwo = new StringBuffer(
                    voucherQueryOne
                            + inquery.toString()
                            +
                            " GROUP BY  vh.vouchernumber, miscbilldtl.billnumber , remgldtl.remittedamt,    gldtl.detailtypeid , gldtl.detailkeyid,"
                            +
                            " remdtl.ID,vh.id,gld.voucherheaderid,billmis.BILLID");
            final Query sqlVoucherQuery = persistenceService.getSession().createSQLQuery(voucherQueryTwo.toString())
                    .addScalar("remittedAmount").addScalar("billAmount").addScalar("voucherNumber")
                    .addScalar("billNumber").addScalar("remittanceDTId")
                    .addScalar("detailKeyTypeId").addScalar("detailKeyId").addScalar("voucherId").addScalar("billId")
                    .setResultTransformer(Transformers.aliasToBean(AutoRemittanceBeanReport.class));
            autoRemittance.addAll(sqlVoucherQuery.list());
        }
        final ArrayList<AutoRemittanceBeanReport> autoRemittanceList = new ArrayList<AutoRemittanceBeanReport>();
        for (final AutoRemittanceBeanReport remittance : remittanceList)
            for (final AutoRemittanceBeanReport autoremit : autoRemittance)
                if (autoremit.getRemittanceDTId().intValue() == remittance.getRemittanceDTId().intValue()) {
                    final AutoRemittanceBeanReport autoRemittanceBeannReport = new AutoRemittanceBeanReport();

                    autoRemittanceBeannReport.setRemittancePaymentNo(remittance.getRemittancePaymentNo());
                    autoRemittanceBeannReport.setRtgsNoDate(remittance.getRtgsNoDate());
                    autoRemittanceBeannReport.setRtgsAmount(remittance.getRtgsAmount());
                    autoRemittanceBeannReport.setDepartment(remittance.getDepartment());
                    autoRemittanceBeannReport.setDrawingOfficer(remittance.getDrawingOfficer());
                    autoRemittanceBeannReport.setFundName(remittance.getFundName());
                    autoRemittanceBeannReport.setBankbranchAccount(remittance.getBankbranchAccount());
                    autoRemittanceBeannReport.setRemittanceCOA(remittance.getRemittanceCOA());
                    autoRemittanceBeannReport.setPaymentVoucherId(remittance.getPaymentVoucherId());
                    autoRemittanceBeannReport.setBillId(remittance.getBillId());

                    autoRemittanceBeannReport.setVoucherNumber(autoremit.getVoucherNumber());
                    autoRemittanceBeannReport.setBillAmount(autoremit.getBillAmount());
                    autoRemittanceBeannReport.setBillNumber(autoremit.getBillNumber());
                    autoRemittanceBeannReport.setRemittedAmount(autoremit.getRemittedAmount());
                    autoRemittanceBeannReport.setVoucherId(autoremit.getVoucherId());
                    autoRemittanceBeannReport.setBillId(autoremit.getBillId());
                    final EntityType entity = voucherHibDAO.getEntityInfo(new Integer(autoremit.getDetailKeyId().toString()),
                            new Integer(autoremit.getDetailKeyTypeId().toString()));
                    if (entity == null) {
                        LOGGER.error("Entity Might have been deleted........................");
                        LOGGER.error("The detail key " + Integer.valueOf(autoremit.getDetailKeyId().toString())
                                + " of detail type " + Integer.valueOf(autoremit.getDetailKeyTypeId().toString())
                                + "Missing in voucher" + autoremit.getVoucherNumber());
                        throw new ValidationException(Arrays.asList(new ValidationError(
                                "Entity information not available for voucher " + autoremit.getVoucherNumber(),
                                "Entity information not available for voucher " + autoremit.getVoucherNumber())));
                    }
                    autoRemittanceBeannReport.setPartyName(entity.getName());
                    autoRemittanceList.add(autoRemittanceBeannReport);
                }
        return autoRemittanceList;
    }
    
    public boolean validateRtgsForRemittedBean(RemittanceBean remittedBean){
        final List<ValidationError> errors = new ArrayList<ValidationError>();
        Recovery recovery = (Recovery)persistenceService.getSession().createCriteria(Recovery.class).add(Restrictions.idEq(remittedBean.getRecoveryId())).uniqueResult();
        if(recovery != null){
            if(recovery.getAccountNumber() == null || recovery.getAccountNumber().isEmpty() || recovery.getIfscCode() == null || recovery.getIfscCode().isEmpty()){
                errors.add(new ValidationError("RTGS not allowed",
                        "Bank Account or IFSC code are not mapped with Recovery : " + recovery.getRecoveryName()));
                throw new ValidationException(errors);
            }
        }
        return true;
    }
}