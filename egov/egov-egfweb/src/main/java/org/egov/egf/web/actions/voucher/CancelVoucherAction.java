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
package org.egov.egf.web.actions.voucher;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.egov.commons.CVoucherHeader;
import org.egov.commons.Functionary;
import org.egov.commons.Fund;
import org.egov.commons.Fundsource;
import org.egov.commons.Scheme;
import org.egov.commons.SubScheme;
import org.egov.commons.Vouchermis;
import org.egov.commons.dao.FinancialYearDAO;
import org.egov.egf.commons.VoucherSearchUtil;
import org.egov.egf.dashboard.event.FinanceEventType;
import org.egov.egf.dashboard.event.listener.FinanceDashboardService;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.infra.admin.master.entity.Boundary;
import org.egov.infra.admin.master.entity.Department;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.microservice.utils.MicroserviceUtils;
import org.egov.infra.validation.exception.ValidationError;
import org.egov.infra.validation.exception.ValidationException;
import org.egov.infra.web.struts.actions.BaseFormAction;
import org.egov.infra.web.struts.annotation.ValidationErrorPage;
import org.egov.infstr.services.PersistenceService;
import org.egov.infstr.utils.EgovMasterDataCaching;
import org.egov.model.bills.EgBillregistermis;
import org.egov.services.payment.PaymentService;
import org.egov.utils.FinancialConstants;
import org.egov.utils.VoucherHelper;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.exilant.GLEngine.ChartOfAccounts;

@ParentPackage("egov")
@Results({ @Result(name = CancelVoucherAction.SEARCH, location = "cancelVoucher-search.jsp") })
public class CancelVoucherAction extends BaseFormAction {
	@Autowired
	@Qualifier("persistenceService")
	private PersistenceService persistenceService;
	
	@Autowired
        @Qualifier("voucherHelper")
        private VoucherHelper voucherHelpers;
	
	private static final long serialVersionUID = -8065315728701853083L;
	private static final Logger LOGGER = Logger.getLogger(CancelVoucherAction.class);
	public static final Locale LOCALE = new Locale("en", "IN");
	public static final SimpleDateFormat DDMMYYYYFORMATS = new SimpleDateFormat("dd/MM/yyyy", LOCALE);
	private static final String DD_MMM_YYYY = "dd-MMM-yyyy";
	@Autowired
	private CancelBillAndVoucher cancelBillAndVoucher;
	private final List<String> headerFields = new ArrayList<String>();
	private final List<String> mandatoryFields = new ArrayList<String>();
	private CVoucherHeader voucherHeader = new CVoucherHeader();
	private Map<String, String> nameMap;
	private VoucherSearchUtil voucherSearchUtil;
	private PaymentService paymentService;
	private Date fromDate;
	private Date toDate;
	private Fund fundId;
	private String voucherNumber;
	private Long[] selectedVhs;
	protected static final String SEARCH = "search";
	Integer loggedInUser;
	public List<CVoucherHeader> voucherSearchList = new ArrayList<CVoucherHeader>();
	private PersistenceService<CVoucherHeader, Long> cVoucherHeaderPersistanceService;
	List<CVoucherHeader> voucherList;
	List<String> voucherTypes = VoucherHelper.VOUCHER_TYPES;
	Map<String, List<String>> voucherNames;
	private FinancialYearDAO financialYearDAO;
	 @Autowired
	 @Qualifier("chartOfAccounts")
	private ChartOfAccounts chartOfAccounts;

	@Autowired
	private AppConfigValueService appConfigValueService;
	@Autowired
	private MicroserviceUtils microserviceUtils;
	@Autowired
	private EgovMasterDataCaching masterDataCache;
	private Department deptImpl = new Department();
	
	@Autowired
	FinanceDashboardService finDashboardService;

	public CancelVoucherAction() {
		voucherHeader.setVouchermis(new Vouchermis());
		// addRelatedEntity("vouchermis.departmentid", Department.class);
		addRelatedEntity("fundId", Fund.class);
		addRelatedEntity("vouchermis.schemeid", Scheme.class);
		addRelatedEntity("vouchermis.subschemeid", SubScheme.class);
		addRelatedEntity("vouchermis.functionary", Functionary.class);
		addRelatedEntity("vouchermis.divisionid", Boundary.class);
		addRelatedEntity("fundsourceId", Fundsource.class);
	}

	@Override
	public Object getModel() {
		return voucherHeader;
	}

	@Override
	public void prepare() {

		loggedInUser = ApplicationThreadLocals.getUserId().intValue();
		super.prepare();
		getHeaderFields();
		loadDropDowns();
	}

	@SkipValidation
	@Action(value = "/voucher/cancelVoucher-beforeSearch")
	public String beforeSearch() {
		voucherHeader.reset();
		setFromDate(null);
		setToDate(null);
		return SEARCH;
	}

	@ValidationErrorPage(value = SEARCH)
	@Action(value = "/voucher/cancelVoucher-search")
	public String search() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("...Searching for voucher of type " + voucherHeader.getType());
		voucherHeader.getVouchermis().setDepartmentcode(deptImpl.getCode());
		voucherSearchList = getVouchersForCancellation();
		List<org.egov.infra.microservice.models.Department> departments = masterDataCache.get("egi-department");
		Map<String, String> depMap = new HashMap<>();
		for (org.egov.infra.microservice.models.Department department : departments) {
			depMap.put(department.getCode(), department.getName());
		}
		if (voucherSearchList != null && !voucherSearchList.isEmpty())
			for (CVoucherHeader voucher : voucherSearchList) {
				if (voucher.getVouchermis()!=null && voucher.getVouchermis().getDepartmentcode() != null)
					voucher.setDepartmentName(depMap.get(voucher.getVouchermis().getDepartmentcode()));
			}
		return SEARCH;
	}

	private boolean isSuperUser() {
		final Query queryFnd = persistenceService.getSession().createSQLQuery(new StringBuilder(" SELECT usrr.USERID")
                .append(" FROM EG_USERROLE usrr,  EG_ROLE r")
                .append(" WHERE usrr.ROLEID=r.ID and usrr.userid = :userId AND lower(r.NAME)=:name").toString());
        final List<Object> superUserList = queryFnd.setParameter("userId", loggedInUser, IntegerType.INSTANCE)
                .setParameter("name", FinancialConstants.SUPERUSER, StringType.INSTANCE)
                .list();
		if (superUserList != null && superUserList.size() > 0)
			return true;
		else
			return false;
	}

	@SuppressWarnings("unchecked")
	private List<CVoucherHeader> getVouchersForCancellation() {
		StringBuilder voucheerWithNoPayment= new StringBuilder();
		StringBuilder allPayment = new StringBuilder();
		StringBuilder noChequePaymentQry = new StringBuilder();
		String contraVoucherQry;
		String filterQry = "";
		if (!voucherNumber.isEmpty() && voucherNumber != null) {
			CVoucherHeader vocuHeaders = (CVoucherHeader) persistenceService
					.find(" from CVoucherHeader vh where vh.voucherNumber = ? and vh.status = 0", voucherNumber);
			if (vocuHeaders != null && vocuHeaders.getVoucherDate() != null) {
				validateBeforeCancel(vocuHeaders);
			}
		} else {
			final boolean validateFinancialYearForPosting = voucherSearchUtil.validateFinancialYearForPosting(fromDate,
					toDate);

			final boolean validateClosedPeriod = voucherSearchUtil.validateClosedPeriod(fromDate, toDate);
			if ((!validateFinancialYearForPosting) || (!validateClosedPeriod))
				throw new ValidationException(Arrays.asList(new ValidationError(
						"Financial Year  Not active for Posting(either year or date within selected date range)",
						"Financial Year  Not active for Posting(either year or date within selected date range)")));
		}

		final Map<String, Object> params = new HashMap<>();
		final String filter = voucherSearchUtil.voucherFilterQuery(voucherHeader, fromDate, toDate, "", params);
		final String userCond = "";
		voucherList = new ArrayList<CVoucherHeader>();
		final List<CVoucherHeader> toBeRemovedList = new ArrayList<CVoucherHeader>();
		filterQry = filter.concat(userCond).concat(" order by vh.voucherNumber");

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("......Searching voucher for cancelllation...");
		if (voucherHeader.getVoucherNumber() != null && !voucherHeader.getVoucherNumber().isEmpty()) {

			CVoucherHeader cVoucherByVoucherNumber = this.getCVoucherByVoucherNumber(voucherHeader.getVoucherNumber());
			if (cVoucherByVoucherNumber != null && cVoucherByVoucherNumber.getType()
					.equalsIgnoreCase(FinancialConstants.STANDARD_VOUCHER_TYPE_PAYMENT)) {
				// Checking the searched BPV whether cheque is issued or not
				final StringBuilder voucherNumberQry = new StringBuilder(
						"from CVoucherHeader vh where vh.status not in (:vhStatus)")
								.append(" and ( vh.isConfirmed != 1 or vh.isConfirmed is null) ").append(filter)
								.append(" and not Exists(select 'true' from InstrumentVoucher iv")
								.append(" where iv.voucherHeaderId=vh.id) order by vh.voucherNumber");

				final Query voucherNumQuery = persistenceService.getSession().createQuery(voucherNumberQry.toString());
				voucherNumQuery.setParameter("vhStatus", Arrays.asList(FinancialConstants.PREAPPROVEDVOUCHERSTATUS,
						FinancialConstants.CANCELLEDVOUCHERSTATUS));
				params.entrySet().forEach(entry -> voucherNumQuery.setParameter(entry.getKey(), entry.getValue()));
				voucherList.addAll(voucherNumQuery.list());

				// If cheque is assigned to BPV then cheque is surrendered or not
				List<Object[]> listOfIdsOfSurrenderedCheques = this.getListOfIdsOfSurrenderedCheques(filter, params);
				if (!listOfIdsOfSurrenderedCheques.isEmpty()) {
					for (Object[] obj : listOfIdsOfSurrenderedCheques) {
						if (voucherHeader.getVoucherNumber().equalsIgnoreCase(obj[1].toString())) {
							voucherList.add((CVoucherHeader) persistenceService.find(
									"from CVoucherHeader vh where vh.id=? and vh.status = 0",
									Long.parseLong(obj[0].toString())));
							break;
						}
					}

				}
			} else if (cVoucherByVoucherNumber != null && cVoucherByVoucherNumber.getType()
					.equalsIgnoreCase(FinancialConstants.STANDARD_VOUCHER_TYPE_JOURNAL)) {
				// Checking Voucher for which paymet is generated or not
				voucheerWithNoPayment = new StringBuilder(
						"from CVoucherHeader vh where vh not in ( select billVoucherHeader from Miscbilldetail)")
								.append(" and vh.status in (?) and (vh.isConfirmed != 1 or vh.isConfirmed is null)")
								.append(" and vh.voucherNumber = ?");

				CVoucherHeader jVoucher = (CVoucherHeader) persistenceService.find(voucheerWithNoPayment.toString(),
						FinancialConstants.CREATEDVOUCHERSTATUS, voucherHeader.getVoucherNumber());
				if (jVoucher != null) {
					voucherList.add(jVoucher);
				} else {
					// Filters vouchers for which payments are generated and are in
					// cancelled state
					allPayment = new StringBuilder(
							"select count(vh) from Miscbilldetail misc, CVoucherHeader vh where misc.billVoucherHeader.id = ? ")
									.append("and misc.payVoucherHeader = vh.id  ")
									.append("and (vh.isConfirmed != 1  or  vh.isConfirmed  is null) ")
									.append("and vh.status in (?)");

					long count = (long) persistenceService.find(allPayment.toString(), cVoucherByVoucherNumber.getId(),
							FinancialConstants.CREATEDVOUCHERSTATUS);
					if (count == 0) {
						voucherList.add(cVoucherByVoucherNumber);
					}
				}
			}
			return voucherList;
		}
		if (voucherHeader.getType().equalsIgnoreCase(FinancialConstants.STANDARD_VOUCHER_TYPE_JOURNAL)) {
			// Voucher for which payment is not generated
			voucheerWithNoPayment = new StringBuilder(
					"from CVoucherHeader vh where vh not in ( select billVoucherHeader from Miscbilldetail)").append(
							" and vh.status in (:vhStatus)  and (vh.isConfirmed != 1 or vh.isConfirmed is null)");
			// Filters vouchers for which payments are generated and are in
			// cancelled state
			allPayment = new StringBuilder("select distinct(vh) from  Miscbilldetail misc left join")
					.append(" misc.billVoucherHeader vh where misc.billVoucherHeader is not null")
					.append(" and (vh.isConfirmed != 1  or  vh.isConfirmed  is null ) and vh.status in (:vhStatus)");

			final Query query = persistenceService.getSession().createQuery((voucheerWithNoPayment.append(filterQry)).toString());
			query.setParameter("vhStatus", FinancialConstants.CREATEDVOUCHERSTATUS);
			params.entrySet().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));
			voucherList.addAll(query.list());

			final Query allPaymentQuery = persistenceService.getSession().createQuery((allPayment.append(filterQry)).toString());
			allPaymentQuery.setParameter("vhStatus", FinancialConstants.CREATEDVOUCHERSTATUS);
			params.entrySet().forEach(entry -> allPaymentQuery.setParameter(entry.getKey(), entry.getValue()));
			voucherList.addAll(allPaymentQuery.list());

			// editModeQuery3 :-check for voucher for for which payments are
			// active 0,5 this will be removed from above two list
			final String editModeQuery3 = new StringBuilder(
					" select misc.billVoucherHeader.id from CVoucherHeader ph, Miscbilldetail misc,CVoucherHeader vh  where ")
							.append(" misc.payVoucherHeader=ph and   misc.billVoucherHeader is not null and misc.billVoucherHeader=vh ")
							.append(" and ph.status  in (:phStatus) and (vh.isConfirmed != 1 or  vh.isConfirmed is null) ")
							.toString();

			final Query editModeQueryThree = persistenceService.getSession().createQuery(editModeQuery3.concat(filterQry));
			editModeQueryThree.setParameter("phStatus", Arrays.asList(FinancialConstants.CREATEDVOUCHERSTATUS,
					FinancialConstants.PREAPPROVEDVOUCHERSTATUS));
			params.entrySet().forEach(entry -> editModeQueryThree.setParameter(entry.getKey(), entry.getValue()));

			final List<Long> vouchersHavingActivePayments = editModeQueryThree.list();

			// If remittance payment is there and are in cancelled state
			final StringBuilder uncancelledRemittances = new StringBuilder(
					" SELECT distinct(vh.id) FROM EgRemittanceDetail r, EgRemittanceGldtl rgd, Generalledgerdetail gld, ")
							.append(" CGeneralLedger gl, EgRemittance rd, CVoucherHeader vh ,Vouchermis billmis,")
							.append(" CVoucherHeader remittedvh  WHERE ")
							.append(" r.egRemittanceGldtl=rgd AND rgd.generalledgerdetail=gld AND gld.generalledger=gl")
							.append(" AND r.egRemittance=rd AND")
							.append(" rd.voucherheader=remittedvh AND gl.voucherHeaderId =vh  AND ")
							.append(" remittedvh =billmis.voucherheaderid and remittedvh.status!=:status");

			final Query remittanceQuery = persistenceService.getSession()
					.createQuery(uncancelledRemittances + filter + userCond);
			remittanceQuery.setParameter("status", FinancialConstants.CANCELLEDVOUCHERSTATUS);
			params.entrySet().forEach(entry -> remittanceQuery.setParameter(entry.getKey(), entry.getValue()));

			final List<Long> remittanceBillVhIdList = remittanceQuery.list();

			remittanceBillVhIdList.addAll(vouchersHavingActivePayments);

			// If remmittacnce payment is generated remove the voucher from the
			// list
			if (voucherList != null && voucherList.size() != 0 && remittanceBillVhIdList != null
					&& remittanceBillVhIdList.size() != 0) {
				for (int i = 0; i < voucherList.size(); i++)
					if (remittanceBillVhIdList.contains(voucherList.get(i).getId()))
						toBeRemovedList.add(voucherList.get(i));
				for (final CVoucherHeader vh : toBeRemovedList)
					voucherList.remove(vh);
			}
		} else if (voucherHeader.getType().equalsIgnoreCase(FinancialConstants.STANDARD_VOUCHER_TYPE_PAYMENT)) {
			final String qryStr = filter;
			String filterQuerySql = "";
			String misTab = "";
			String VoucherMisJoin = "";
			if (qryStr.contains(" and vh.vouchermis")) {
				misTab = ", vouchermis mis ";
				VoucherMisJoin = " and vh.id=mis.voucherheaderid";
				filterQuerySql = qryStr.replace("and vh.vouchermis.", " and mis.");
			} else
				filterQuerySql = filter;
			// BPVs for which no Cheque is issued
			noChequePaymentQry = noChequePaymentQry
					.append("from CVoucherHeader vh where vh.status not in (:vhStatus)  ").append(filter)
					.append("  and not Exists(select 'true' from InstrumentVoucher iv where iv.voucherHeaderId=vh.id)")
					.append(" order by vh.voucherNumber)");

			final Query noChequePaymentQuery = persistenceService.getSession()
					.createQuery(noChequePaymentQry.toString());
			noChequePaymentQuery.setParameterList("vhStatus", Arrays.asList(FinancialConstants.PREAPPROVEDVOUCHERSTATUS,
					FinancialConstants.CANCELLEDVOUCHERSTATUS));
			persistenceService.populateQueryWithParams(noChequePaymentQuery, params);

			voucherList.addAll(noChequePaymentQuery.list());

			// Query for cancelling BPVs for which cheque is assigned and
			// cancelled
			filterQuerySql = filterQuerySql.replace(" and vh.fundId.id=:fundId", " and vh.fundId=:fundId");
			
			final Query query1 = persistenceService.getSession()
					.createSQLQuery(new StringBuilder("SELECT distinct vh.id FROM egw_status status").append(misTab)
							.append(", voucherheader vh 	")
							.append(" LEFT JOIN EGF_INSTRUMENTVOUCHER IV ON VH.ID=IV.VOUCHERHEADERID")
							.append("	LEFT JOIN EGF_INSTRUMENTHEADER IH ON IV.INSTRUMENTHEADERID=IH.ID INNER JOIN ")
							.append("(SELECT MAX(iv1.instrumentheaderid) AS maxihid, iv1.voucherheaderid  AS iv1vhid")
							.append(" FROM egf_instrumentvoucher iv1 GROUP BY iv1.voucherheaderid)as INST ON maxihid=IH.ID ")
							.append(" WHERE	IV.VOUCHERHEADERID  IS NOT NULL	AND status.description   IN (:description)")
							.append(" and status.id=ih.id_status and vh.status not in (:vhStatus) ")
							.append(VoucherMisJoin).append(filterQuerySql).toString());
			
			query1.setParameterList("description",
					Arrays.asList(FinancialConstants.INSTRUMENT_CANCELLED_STATUS,
							FinancialConstants.INSTRUMENT_SURRENDERED_STATUS,
							FinancialConstants.INSTRUMENT_SURRENDERED_FOR_REASSIGN_STATUS))
					.setParameterList("vhStatus", Arrays.asList(FinancialConstants.PREAPPROVEDVOUCHERSTATUS,
							FinancialConstants.CANCELLEDVOUCHERSTATUS));

			persistenceService.populateQueryWithParams(query1, params);

			final List<BigInteger> list = query1.list();

			for (final BigInteger b : list)
				voucherList.add(
						(CVoucherHeader) persistenceService.find("from CVoucherHeader  where id=?", b.longValue()));
		} else if (voucherHeader.getType().equalsIgnoreCase(FinancialConstants.STANDARD_VOUCHER_TYPE_CONTRA)) {
			contraVoucherQry = new StringBuilder("from CVoucherHeader vh where vh.status =:vhStatus")
					.append(" and ( vh.isConfirmed != 1 or vh.isConfirmed is null) and vh.refvhId is null ").toString();
			final Query query = persistenceService.getSession().createQuery(contraVoucherQry + filterQry);
			query.setParameter("vhStatus", FinancialConstants.CREATEDVOUCHERSTATUS);
			persistenceService.populateQueryWithParams(query, params);
			voucherList.addAll(query.list());
		}
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("......No of voucher found in search for is cancellation ..." + voucherList.size());
		return voucherList;
	}

	@SkipValidation
	public void validateBeforeCancel(final CVoucherHeader voucherObj) {
	    final SimpleDateFormat formatter = new SimpleDateFormat(DD_MMM_YYYY);
	    try {
            if (chartOfAccounts.isClosedForPosting(formatter.format(voucherObj.getVoucherDate()))) {
                throw new ValidationException(Arrays.asList(new ValidationError(
                        "Financial Year  Not active for Posting(either year or date within selected date range)",
                        "Financial Year  Not active for Posting(either year or date within selected date range)")));
            }
        } catch (ValidationException e) {
            final List<ValidationError> errors = new ArrayList<ValidationError>();
            errors.add(new ValidationError("exp", e.getErrors().get(0).getMessage()));
            throw new ValidationException(errors);
        }
	}
	
	@ValidationErrorPage(value = SEARCH)
	@SkipValidation
	@Action(value = "/voucher/cancelVoucher-update")
	public String update() {
		final Map<String, Object> map = cancelVouchers(selectedVhs, loggedInUser, voucherHeader);
        ((List<String>) map.get("voucherNumbers")).forEach(rec -> addActionMessage(getText("msg.voucher.cancel.creator", new String[] {rec})));
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(" Cancel Voucher | CancelVoucher | Vouchers Cancelled ");
        if (!map.get("voucherId").equals(""))
            addActionMessage(getText("Vouchers Cancelled Succesfully"));
        return SEARCH;
	}
	
	public Map<String, Object> cancelVouchers(final Long[] selectedVhs, final Integer loggedInUser, final CVoucherHeader voucherHeader) {
        CVoucherHeader voucherObj;
        final Map<String, Object> map = new HashMap<>();
        final Date modifiedDate = new Date();
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Inside CancelVoucher| cancelVoucherSubmit | Selected No of Vouchers for cancellation  ="
                    + selectedVhs.length);
        final String cancelVhQuery = "Update CVoucherHeader vh set vh.status=:vhStatus, vh.lastModifiedBy=:modifiedby, vh.lastModifiedDate=:modifiedDate where vh.id=:vhId";
        final String cancelVhByCGNQuery = "Update CVoucherHeader vh set vh.status=:vhStatus, vh.lastModifiedBy=:modifiedby, vh.lastModifiedDate=:modifiedDate where vh.refvhId=:vhId";
        final String cancelVhByRefCGNQuery = "Update CVoucherHeader vh set vh.status=:vhStatus, vh.lastModifiedBy=:modifiedby, vh.lastModifiedDate=:modifiedDate where vh.voucherNumber=:vhNum";
        String voucherId = "";
        Set<Long> ids = new HashSet<>();
        final Session session = persistenceService.getSession();
        final List<String> voucherNumbers = new ArrayList<>();
        for (int i = 0; i < selectedVhs.length; i++) {
            voucherObj = (CVoucherHeader) persistenceService.find("from CVoucherHeader vh where vh.id=?", selectedVhs[i]);
            ids.add(selectedVhs[i]);
            final boolean value = cancelBillAndVoucher.canCancelVoucher(voucherObj);

            if (!value) {
                voucherNumbers.add(voucherObj.getVoucherNumber());
                continue;
            }
            voucherId = voucherObj.getId().toString();
            switch (voucherObj.getType()) {

            case FinancialConstants.STANDARD_VOUCHER_TYPE_JOURNAL: {

                final Query query = session.createQuery(cancelVhQuery);
                query.setParameter("modifiedby", loggedInUser, IntegerType.INSTANCE)
                        .setParameter("modifiedDate", modifiedDate, TimestampType.INSTANCE)
                        .setParameter("vhId", selectedVhs[i], LongType.INSTANCE)
                        .setParameter("vhStatus", FinancialConstants.CANCELLEDVOUCHERSTATUS, IntegerType.INSTANCE);
                query.executeUpdate();
                // for old vouchers when workflow was not implemented
                
                if(this.isBillGeneratedBySystemForVocuher(voucherObj) && !voucherObj.getName().equals(FinancialConstants.JOURNALVOUCHER_NAME_GENERAL)){
				    cancelBill(selectedVhs[i]);
				}
                break;
            }
            case FinancialConstants.STANDARD_VOUCHER_TYPE_PAYMENT: {
                final Query query = session.createQuery(cancelVhQuery)
                        .setParameter("vhId", selectedVhs[i], LongType.INSTANCE)
                        .setParameter("modifiedby", loggedInUser, IntegerType.INSTANCE)
                        .setParameter("modifiedDate", modifiedDate, TimestampType.INSTANCE)
                        .setParameter("vhStatus", FinancialConstants.CANCELLEDVOUCHERSTATUS, IntegerType.INSTANCE);
                query.executeUpdate();
                if (FinancialConstants.PAYMENTVOUCHER_NAME_REMITTANCE.equalsIgnoreCase(voucherObj.getName())) {
                	paymentService.backUpdateRemittanceDateInGL(voucherHeader.getId());
                }
                break;
            }
            case FinancialConstants.STANDARD_VOUCHER_TYPE_CONTRA: {
                final Query query = session.createQuery(cancelVhQuery);
                query.setParameter("vhId", selectedVhs[i], LongType.INSTANCE)
                        .setParameter("modifiedby", loggedInUser, IntegerType.INSTANCE)
                        .setParameter("modifiedDate", modifiedDate, TimestampType.INSTANCE)
                        .setParameter("vhStatus", FinancialConstants.CANCELLEDVOUCHERSTATUS, IntegerType.INSTANCE);
                query.executeUpdate();
                if (FinancialConstants.CONTRAVOUCHER_NAME_INTERFUND.equalsIgnoreCase(voucherObj.getName())) {
                    Long vhId;
                    vhId = voucherObj.getId();
                    final Query queryFnd = session.createQuery(cancelVhByCGNQuery);
                    queryFnd.setParameter("vhId", vhId, LongType.INSTANCE)
                            .setParameter("modifiedby", loggedInUser, IntegerType.INSTANCE)
                            .setParameter("modifiedDate", modifiedDate, DateType.INSTANCE)
                            .setParameter("vhStatus", FinancialConstants.CANCELLEDVOUCHERSTATUS, IntegerType.INSTANCE);
                    queryFnd.executeUpdate();
                }
                break;
            }
            case FinancialConstants.STANDARD_VOUCHER_TYPE_RECEIPT: {
                final Query query = session.createQuery(cancelVhQuery);
                query.setParameter("vhId", selectedVhs[i], LongType.INSTANCE)
                        .setParameter("modifiedby", loggedInUser, IntegerType.INSTANCE)
                        .setParameter("modifiedDate", modifiedDate, TimestampType.INSTANCE)
                        .setParameter("vhStatus", FinancialConstants.CANCELLEDVOUCHERSTATUS, IntegerType.INSTANCE);
                query.executeUpdate();
                break;
            }
            }
        }
        if(!ids.isEmpty()){
		    finDashboardService.publishEvent(FinanceEventType.voucherUpdateById, ids);
		}
        map.put("voucherNumbers", voucherNumbers);
        map.put("voucherId", voucherId);
        return map;
    }

	private void cancelBill(final Long vhId) {
        final StringBuffer billQuery = new StringBuffer();
        final String statusQuery = "(select stat.id from  egw_status  stat where stat.moduletype=:module and stat.description=:description)";
        final String cancelQuery = new StringBuilder("Update eg_billregister set billstatus=:billstatus, statusid =").append(statusQuery)
                .append(" where  id=:billId").toString();
        String moduleType = "", description = "", billstatus = "";
        final EgBillregistermis billMis = (EgBillregistermis) persistenceService.find(
                "from  EgBillregistermis  mis where voucherHeader.id=?", vhId);

        if (billMis != null && billMis.getEgBillregister().getState() == null) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("....Cancelling Bill Associated with the Voucher....");
            billQuery.append("select bill.expendituretype,bill.id,bill.state.id from CVoucherHeader vh,EgBillregister bill ,EgBillregistermis mis")
                    .append(" where vh.id=mis.voucherHeader and bill.id=mis.egBillregister and vh.id=? ");
            final Object[] bill = (Object[]) persistenceService.find(billQuery.toString(),vhId); // bill[0] contains expendituretype
                                                                                            // and
            // bill[1] contaons billid

            if (FinancialConstants.STANDARD_EXPENDITURETYPE_SALARY.equalsIgnoreCase(bill[0].toString())) {
                billstatus = FinancialConstants.SALARYBILL;
                description = FinancialConstants.SALARYBILL_CANCELLED_STATUS;
                moduleType = FinancialConstants.SALARYBILL;
            } else if (FinancialConstants.STANDARD_EXPENDITURETYPE_CONTINGENT.equalsIgnoreCase(bill[0].toString())) {
                for (String retval : FinancialConstants.EXCLUDED_BILL_TYPES.split(",")) {
                    retval = retval.replace("'", "");
                    if (billMis.getEgBillSubType() != null && billMis.getEgBillSubType().getName().equalsIgnoreCase(retval))
                        return;
                }
                billstatus = FinancialConstants.CONTINGENCYBILL_CANCELLED_STATUS;
                description = FinancialConstants.CONTINGENCYBILL_CANCELLED_STATUS;
                moduleType = FinancialConstants.CONTINGENCYBILL_FIN;
            }

            else if (FinancialConstants.STANDARD_EXPENDITURETYPE_PURCHASE.equalsIgnoreCase(bill[0].toString())) {
                billstatus = FinancialConstants.SUPPLIERBILL_CANCELLED_STATUS;
                description = FinancialConstants.SUPPLIERBILL_CANCELLED_STATUS;
                moduleType = FinancialConstants.SUPPLIERBILL;
            } else if (FinancialConstants.STANDARD_EXPENDITURETYPE_WORKS.equalsIgnoreCase(bill[0].toString())) {
                billstatus = FinancialConstants.CONTRACTORBILL_CANCELLED_STATUS;
                description = FinancialConstants.CONTRACTORBILL_CANCELLED_STATUS;
                moduleType = FinancialConstants.CONTRACTORBILL;
            }
            // pension vouchers created fron financials cancel bill also
            else if (FinancialConstants.STANDARD_EXPENDITURETYPE_PENSION.equalsIgnoreCase(bill[0].toString()))
                if ((Integer) bill[2] == null) {
                    billstatus = FinancialConstants.PENSIONBILL_CANCELLED_STATUS;
                    description = FinancialConstants.PENSIONBILL_CANCELLED_STATUS;
                    moduleType = FinancialConstants.PENSIONBILL;
                }

            final Query billQry = persistenceService.getSession().createSQLQuery(cancelQuery.toString());
            billQry.setParameter("module", moduleType, StringType.INSTANCE)
                    .setParameter("description", description, StringType.INSTANCE)
                    .setParameter("billstatus", billstatus, StringType.INSTANCE)
                    .setParameter("billId", (Long) bill[1], LongType.INSTANCE);
            billQry.executeUpdate();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Bill Cancelled Successfully" + bill[1]);
            
            finDashboardService.publishEvent(FinanceEventType.billUpdateByIds, new HashSet<>(Arrays.asList(bill[1])));
        }
    }
	
	private void loadDropDowns() {

		if (headerFields.contains("department")) {
			List<org.egov.infra.microservice.models.Department> departments = masterDataCache.get("egi-department");
			addDropdownData("departmentList", departments);
		}
		if (headerFields.contains("functionary"))
			addDropdownData("functionaryList",
					persistenceService.findAllBy(" from Functionary where isactive=true order by name"));
		if (headerFields.contains("fund"))
			addDropdownData("fundList",
					persistenceService.findAllBy(" from Fund where isactive=true and isnotleaf=false order by name"));
		if (headerFields.contains("fundsource"))
			addDropdownData("fundsourceList",
					persistenceService.findAllBy(" from Fundsource where isactive=true order by name"));
		if (headerFields.contains("field"))
			addDropdownData("fieldList",
					persistenceService.findAllBy(" from Boundary b where lower(b.boundaryType.name)='ward' "));
		if (headerFields.contains("scheme"))
			addDropdownData("schemeList", Collections.EMPTY_LIST);
		if (headerFields.contains("subscheme"))
			addDropdownData("subschemeList", Collections.EMPTY_LIST);
		// addDropdownData("typeList",
		// persistenceService.findAllBy(" select distinct vh.type from
		// CVoucherHeader vh order by vh.type")); //where
		// vh.status!=4
		addDropdownData("typeList", VoucherHelper.VOUCHER_TYPES);
		voucherNames = voucherHelpers.getVoucherNamesAndTypes();
		nameMap = new LinkedHashMap<String, String>();
	}

	@Override
	public void validate() {
		if (voucherHeader.getVoucherNumber() == null || voucherHeader.getVoucherNumber().isEmpty()) {
			if (fromDate == null)
				addFieldError("From Date", getText("Please Enter From Date"));
			if (toDate == null)
				addFieldError("To Date", getText("Please Enter To Date"));
			if (voucherHeader.getType() == null || voucherHeader.getType().equals("-1"))
				addFieldError("Voucher Type", getText("Please Select Voucher Type"));
			if (voucherHeader.getName() == null || voucherHeader.getName().equals("-1")
					|| voucherHeader.getName().equals("0"))
				addFieldError("Voucher Type", getText("Please Select Voucher Name"));
			int checKDate = 0;
			if (fromDate != null && toDate != null)
				checKDate = fromDate.compareTo(toDate);
			if (checKDate > 0)
				addFieldError("To Date", getText("Please Enter To Date Greater than From Date"));
			checkMandatoryField("fundId", "fund", voucherHeader.getFundId(), "voucher.fund.mandatory");
		}
		checkMandatoryField("vouchermis.departmentcode", "department",
				voucherHeader.getVouchermis().getDepartmentcode(), "voucher.department.mandatory");
		checkMandatoryField("vouchermis.schemeid", "scheme", voucherHeader.getVouchermis().getSchemeid(),
				"voucher.scheme.mandatory");
		checkMandatoryField("vouchermis.subschemeid", "subscheme", voucherHeader.getVouchermis().getSubschemeid(),
				"voucher.subscheme.mandatory");
		checkMandatoryField("vouchermis.functionary", "functionary", voucherHeader.getVouchermis().getFunctionary(),
				"voucher.functionary.mandatory");
		checkMandatoryField("fundsourceId", "fundsource", voucherHeader.getVouchermis().getFundsource(),
				"voucher.fundsource.mandatory");
		checkMandatoryField("vouchermis.divisionId", "field", voucherHeader.getVouchermis().getDivisionid(),
				"voucher.field.mandatory");
	}

	protected void getHeaderFields() {
		final List<AppConfigValues> appConfigList = appConfigValueService.getConfigValuesByModuleAndKey("EGF",
				"DEFAULT_SEARCH_MISATTRRIBUTES");

		for (final AppConfigValues appConfigVal : appConfigList) {
			final String value = appConfigVal.getValue();
			final String header = value.substring(0, value.indexOf('|'));
			headerFields.add(header);
			final String mandate = value.substring(value.indexOf('|') + 1);
			if (mandate.equalsIgnoreCase("M"))
				mandatoryFields.add(header);
		}

	}

	protected void checkMandatoryField(final String objectName, final String fieldName, final Object value,
			final String errorKey) {
		if (mandatoryFields.contains(fieldName) && (value == null || value.equals(-1)))
			addFieldError(objectName, getText(errorKey));
	}
	
	public List<Object[]> getListOfIdsOfSurrenderedCheques(String filter, Map<String, Object> params) {
		final String qryStr = filter;
		String filterQuerySql = "";
		String misTab = "";
		String VoucherMisJoin = "";
		if (qryStr.contains(" and vh.vouchermis")) {
			misTab = ", vouchermis mis ";
			VoucherMisJoin = " and vh.id=mis.voucherheaderid";
			filterQuerySql = qryStr.replace("and vh.vouchermis.", " and mis.");
		} else
			filterQuerySql = filter;
		final Query query1 = persistenceService.getSession()
				.createSQLQuery(new StringBuilder("SELECT distinct vh.id, vh.voucherNumber FROM egw_status status")
						.append(misTab)
						.append(", voucherheader vh LEFT JOIN EGF_INSTRUMENTVOUCHER IV ON VH.ID=IV.VOUCHERHEADERID")
						.append(" LEFT JOIN EGF_INSTRUMENTHEADER IH ON IV.INSTRUMENTHEADERID=IH.ID")
						.append(" INNER JOIN (SELECT MAX(iv1.instrumentheaderid) AS maxihid,")
						.append(" iv1.voucherheaderid  AS iv1vhid   FROM egf_instrumentvoucher iv1")
						.append(" GROUP BY iv1.voucherheaderid)as INST ON maxihid=IH.ID ")
						.append(" WHERE IV.VOUCHERHEADERID  IS NOT NULL AND status.description IN (:description)")
						.append(" and status.id=ih.id_status and vh.status not in (:vhStatus) ").append(VoucherMisJoin)
						.append(filterQuerySql).toString());
		query1.setParameterList("description",
				Arrays.asList(FinancialConstants.INSTRUMENT_CANCELLED_STATUS,
						FinancialConstants.INSTRUMENT_SURRENDERED_STATUS,
						FinancialConstants.INSTRUMENT_SURRENDERED_FOR_REASSIGN_STATUS))
				.setParameterList("vhStatus", Arrays.asList(FinancialConstants.PREAPPROVEDVOUCHERSTATUS,
						FinancialConstants.CANCELLEDVOUCHERSTATUS));
		params.entrySet().forEach(entry -> query1.setParameter(entry.getKey(), entry.getValue()));
		return (List<Object[]>) query1.list();
	}
	
	@SuppressWarnings("deprecation")
        public CVoucherHeader getCVoucherByVoucherNumber(String voucherNumber){
	    List<CVoucherHeader> vocuHeaders = persistenceService.findAllBy(" from CVoucherHeader vh where vh.voucherNumber = ? and vh.status = 0", voucherNumber);
	    if(!vocuHeaders.isEmpty()){
	        return vocuHeaders.get(0);
	    }
	    return null;
	}
	
	//this method differentiating between internal/external bill by sourcepath column
	//as sourcepath column is not updating/inserting the data while generation of bill by system(internal)
	public boolean isBillGeneratedBySystemForVocuher(CVoucherHeader voucher){
	    String query = "select billmis.sourcePath from EgBillregistermis billmis where billmis.voucherHeader.id = ?";
	    Object sourcePath = persistenceService.find(query, voucher.getId());
	    return sourcePath == null ? true : false;
	}

	public boolean isFieldMandatory(final String field) {
		return mandatoryFields.contains(field);
	}

	public boolean shouldShowHeaderField(final String field) {
		return headerFields.contains(field);
	}

	public CVoucherHeader getVoucherHeader() {
		return voucherHeader;
	}

	public void setVoucherHeader(final CVoucherHeader voucherHeader) {
		this.voucherHeader = voucherHeader;
	}

	public Map<String, String> getNameMap() {
		return nameMap;
	}

	public void setNameMap(final Map<String, String> nameMap) {
		this.nameMap = nameMap;
	}

	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(final Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(final Date toDate) {
		this.toDate = toDate;
	}

	public VoucherSearchUtil getVoucherSearchUtil() {
		return voucherSearchUtil;
	}

	public void setVoucherSearchUtil(final VoucherSearchUtil voucherSearchUtil) {
		this.voucherSearchUtil = voucherSearchUtil;
	}

	public List<CVoucherHeader> getVoucherSearchList() {
		return voucherSearchList;
	}

	public void setVoucherSearchList(final List<CVoucherHeader> voucherSearchList) {
		this.voucherSearchList = voucherSearchList;
	}

	public Long[] getSelectedVhs() {
		return selectedVhs;
	}

	public void setSelectedVhs(final Long[] selectedVhs) {
		this.selectedVhs = selectedVhs;
	}

	public List<CVoucherHeader> getVoucherList() {
		return voucherList;
	}

	public void setVoucherList(final List<CVoucherHeader> voucherList) {
		this.voucherList = voucherList;
	}

	public List<String> getVoucherTypes() {
		return voucherTypes;
	}

	public void setVoucherTypes(final List<String> voucherTypes) {
		this.voucherTypes = voucherTypes;
	}

	public Map<String, List<String>> getVoucherNames() {
		return voucherNames;
	}

	public void setVoucherNames(final Map<String, List<String>> voucherNames) {
		this.voucherNames = voucherNames;
	}

	public PaymentService getPaymentService() {
		return paymentService;
	}

	public void setPaymentService(final PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	public void setFinancialYearDAO(final FinancialYearDAO financialYearDAO) {
		this.financialYearDAO = financialYearDAO;
	}

	public Department getDeptImpl() {
		return deptImpl;
	}

	public void setDeptImpl(final Department deptImpl) {
		this.deptImpl = deptImpl;
	}
	public String getVoucherNumber() {
	        return voucherNumber;
	 }

	public void setVoucherNumber(String voucherNumber) {
	        this.voucherNumber = voucherNumber;
	  }

}
