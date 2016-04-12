<!-- -------------------------------------------------------------------------------
# eGov suite of products aim to improve the internal efficiency,transparency,
#    accountability and the service delivery of the government  organizations.
# 
#     Copyright (C) <2015>  eGovernments Foundation
# 
#     The updated version of eGov suite of products as by eGovernments Foundation
#     is available at http://www.egovernments.org
# 
#     This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU General Public License as published by
#     the Free Software Foundation, either version 3 of the License, or
#     any later version.
# 
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU General Public License for more details.
# 
#     You should have received a copy of the GNU General Public License
#     along with this program. If not, see http://www.gnu.org/licenses/ or
#     http://www.gnu.org/licenses/gpl.html .
# 
#     In addition to the terms of the GPL license to be adhered to in using this
#     program, the following additional terms are to be complied with:
# 
# 	1) All versions of this program, verbatim or modified must carry this
# 	   Legal Notice.
# 
# 	2) Any misrepresentation of the origin of the material is prohibited. It
# 	   is required that all modified versions of this material be marked in
# 	   reasonable ways as different from the original version.
# 
# 	3) This license does not grant any rights to any user of the program
# 	   with regards to rights under trademark law for use of the trade names
# 	   or trademarks of eGovernments Foundation.
# 
#   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
#------------------------------------------------------------------------------- -->
<%@ include file="/includes/taglibs.jsp" %>
<script src="<egov:url path='resources/js/works.js'/>"></script> 
<html>
<head>
<title><s:text name='page.title.subledgerCode' /></title>
</head>
<body class="simple">
	<div class="new-page-header">
		<s:text name="dw.feasibilityRep.depositCode.header" />
	</div>
	<div class="errorstyle" id="subledgerCode" class="alert alert-danger"
		style="display: none;"></div>
	<s:form action="depositcode-save" theme="simple" name="subledgerCode"
		cssClass="form-horizontal form-groups-bordered">
		<s:token />

		<s:push value="model">
			<s:hidden id="code" name="code" />
			<div id="subledgerCodeError" class="alert alert-danger"
				style="display: none;"></div>
			<s:if test="%{hasErrors()}">
				<div id="errorstyle" class="alert alert-danger">
					<s:actionerror />
					<s:fielderror />
				</div>
			</s:if>
			<s:if test="%{hasActionMessages()}">
				<div class="alert alert-success">
					<s:actionmessage theme="simple" />
				</div>
			</s:if>

			<div class="panel panel-primary" data-collapsed="0" style="text-align: left">
				<div class="panel-heading">
					<div class="panel-title"></div>
				</div>
				<div class="panel-body no-margin-bottom">
					<div class="form-group">
						<label class="col-sm-2 control-label text-right">
							<s:text name="depositCode.work.name" /> <span class="mandatory"></span>
						</label>
						<div class="col-sm-3 add-margin">
							<s:textfield name="codeName" type="text" cssClass="form-control" id="codeName" value="%{codeName}" />
						</div>
						<label class="col-sm-2 control-label text-right">
						<s:text name="depositCode.work.description" />
						</label>
						<div class="col-sm-3 add-margin">
							<s:textarea name="description" cols="35" cssClass="form-control" id="codeDescription" value="%{description}" />
						</div>
					</div>

					<div class="form-group">
						<label id="financialYear1" class="col-sm-2 control-label text-right">
						<s:text name='subledgerCode.financialYear' /> <span class="mandatory"></span>
						</label>
						<div class="col-sm-3 add-margin">
							<s:select headerKey="-1" headerValue="%{getText('default.dropdown.select')}"  name="financialYear" id="financialYear" cssClass="form-control" list="dropdownData.financialYearList" listKey="id"  listValue="finYearRange" value="%{currentFinancialYearId}" />
						</div>
							<label class="col-sm-2 control-label text-right"> <s:text
									name='subledgerCode.fund' /><span class="mandatory"></span>
							</label>
						<div class="col-sm-3 add-margin">
							<s:select headerKey="-1" headerValue="%{getText('default.dropdown.select')}" name="fund"  id="fund" cssClass="form-control" list="dropdownData.fundList"  listKey="id" listValue="name" />
						</div>
					</div>

					<div class="form-group">
						<label class="col-sm-2 control-label text-right"> 
							<s:text name="subledgerCode.fundSource.name" /> <span class="mandatory"></span>
						</label>
						<div class="col-sm-3 add-margin">
							<s:select headerKey="-1" headerValue="%{getText('default.dropdown.select')}" name="fundSource" id="fundSource" cssClass="form-control" list="dropdownData.fundSourceList" listKey="id" listValue="name" value="%{fundSource.id}" />
						</div>
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-xs-12 text-center buttonholdersearch">
					<s:submit cssClass="btn btn-primary" value="Save" id="saveButton" name="saveButton" method="save"  onclick="return validateSubledgerCodeBeforeSubmit();" />
					&nbsp; <input type="button" class="btn btn-default" value="Close"  id="closeButton" name="closeButton"  onclick="confirmClose('<s:text name='subledger.close.confirm'/>');" />
				</div>
			</div>
		</s:push>
	</s:form>
</body>
</html>