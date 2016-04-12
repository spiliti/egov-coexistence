/**
 * eGov suite of products aim to improve the internal efficiency,transparency,
   accountability and the service delivery of the government  organizations.

    Copyright (C) <2015>  eGovernments Foundation

    The updated version of eGov suite of products as by eGovernments Foundation
    is available at http://www.egovernments.org

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see http://www.gnu.org/licenses/ or
    http://www.gnu.org/licenses/gpl.html .

    In addition to the terms of the GPL license to be adhered to in using this
    program, the following additional terms are to be complied with:

        1) All versions of this program, verbatim or modified must carry this
           Legal Notice.

        2) Any misrepresentation of the origin of the material is prohibited. It
           is required that all modified versions of this material be marked in
           reasonable ways as different from the original version.

        3) This license does not grant any rights to any user of the program
           with regards to rights under trademark law for use of the trade names
           or trademarks of eGovernments Foundation.

  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.wtms.web.controller.masters;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;

import javax.validation.Valid;

import org.egov.wtms.masters.entity.PipeSize;
import org.egov.wtms.masters.service.PipeSizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(value = "/masters")
public class PipeSizeMasterController {

    @Autowired
    private PipeSizeService pipeSizeService;

    @RequestMapping(value = "/pipesizeMaster", method = GET)
    public String viewForm(final Model model) {
        final PipeSize pipeSize = new PipeSize();
        model.addAttribute("pipeSize", pipeSize);
        model.addAttribute("reqAttr", false);
        return "pipesize-master";
    }

    @RequestMapping(value = "/pipesizeMaster", method = RequestMethod.POST)
    public String addCategoryMasterData(@Valid @ModelAttribute final PipeSize pipeSize,
            final RedirectAttributes redirectAttrs, final Model model, final BindingResult resultBinder) {
        if (resultBinder.hasErrors())
            return "pipesize-master";
        final PipeSize pipesizecodeObj = pipeSizeService.findByCode(pipeSize.getCode());
        final PipeSize pipesizemmObj = pipeSizeService.findBySizeInMilimeter(pipeSize.getSizeInMilimeter());
        final PipeSize pipesizeObj = pipeSizeService.findByCodeAndPipeSizeInmm(pipeSize.getCode(),
                pipeSize.getSizeInMilimeter());

        if (pipesizeObj != null) {
            redirectAttrs.addFlashAttribute("pipeSize", pipesizeObj);
            model.addAttribute("message", "Entered Code and H.S.C Pipe Size(mm) are already exists.");
        } else if (pipesizecodeObj != null) {
            redirectAttrs.addFlashAttribute("pipeSize", pipesizecodeObj);
            model.addAttribute("message", "Entered Code already exist.");
        } else if (pipesizemmObj != null) {
            redirectAttrs.addFlashAttribute("pipeSize", pipesizemmObj);
            model.addAttribute("message", "Entered  H.S.C Pipe Size(mm) already exist.");
            return "pipesize-master";
        } else {
            pipeSizeService.createPipeSize(pipeSize);
            redirectAttrs.addFlashAttribute("pipeSize", pipeSize);

        }

        return getPipeSizeMasterList(model);
    }

    @RequestMapping(value = "/pipesizeMaster/list", method = GET)
    public String getPipeSizeMasterList(final Model model) {
        final List<PipeSize> pipeSizeList = pipeSizeService.findAll();
        model.addAttribute("pipeSizeList", pipeSizeList);
        return "pipesize-master-list";
    }

    @RequestMapping(value = "/pipesizeMaster/{pipeSizeId}", method = GET)
    public String getPipeSizeMasterDetails(final Model model, @PathVariable final String pipeSizeId) {
        final PipeSize pipeSize = pipeSizeService.findOne(Long.parseLong(pipeSizeId));
        model.addAttribute("pipeSize", pipeSize);
        model.addAttribute("reqAttr", "true");
        return "pipesize-master";
    }

    @RequestMapping(value = "/pipesizeMaster/{pipeSizeId}", method = RequestMethod.POST)
    public String editPipeSizeMasterData(@Valid @ModelAttribute final PipeSize pipeSize,
            @PathVariable final long pipeSizeId, final RedirectAttributes redirectAttrs, final Model model,
            final BindingResult resultBinder) {
        if (resultBinder.hasErrors())
            return "pipesize-master";
        pipeSizeService.updatePipeSize(pipeSize);
        redirectAttrs.addFlashAttribute("pipeSize", pipeSize);
        return getPipeSizeMasterList(model);

    }

}
