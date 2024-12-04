package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

public class ExcelFilePreprocessor {

    // Method to preprocess the workbook
    public static void preprocessWorkbook(Workbook workbook) {
        // Check if the first sheet is named "Cover"
        Sheet firstSheet = workbook.getSheetAt(0);
        if ("Cover".equalsIgnoreCase(firstSheet.getSheetName())) {
            // Delete the first 4 rows from each sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                deleteFirstNRows(sheet, 4);
            }
            // Rename each tab
            renameSheets(workbook);
        }
    }

    private static void deleteFirstNRows(Sheet sheet, int n) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum >= n) {
            sheet.shiftRows(n, lastRowNum, -n);
        } else {
            // If there are fewer rows than n, remove all rows
            for (int i = lastRowNum; i >= 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
        }
    }

    private static void renameSheets(Workbook workbook) {
        // Define a mapping from old sheet names to new sheet names
        Map<String, String> sheetNameMapping = new HashMap<>();
        // Add mapping entries: sheetNameMapping.put("OldName", "NewName");
        // You need to fill in the actual mapping based on your requirements

        // Example mapping (replace with your actual mapping)
        sheetNameMapping.put("Cover", "Cover");
        sheetNameMapping.put("Accum ICL & MedD OOP", "Accum ICL&MedD - accumIclOopLst");
        sheetNameMapping.put("Accum Override Copay", "AccumOv - accumOverrideCopayLst");
        sheetNameMapping.put("Accum HRA - acmltnHraIPLst", "Accum HRA - acmltnHraIPLst");
        sheetNameMapping.put("Addl Refill Fill Limits", "AddlRefil - addlRefillLimitsLst");
        sheetNameMapping.put("Alternate Plan", "Alternate Plan - altPlanIPLst");
        sheetNameMapping.put("Base Coverage", "Base Coverage - baseCoverage");
        sheetNameMapping.put("Base Coverage", "Base Coverage - basePlanIPLst");
        sheetNameMapping.put("BrandGeneric", "BrandGeneric - brandGenericLst");
        sheetNameMapping.put("CDH", "CDH - CdhIP");
        sheetNameMapping.put("CDH", "CDH - cdhIPLst");
        sheetNameMapping.put("CDH SBOR", "CDH SBOR - cdhSborIPLst");
        sheetNameMapping.put("CLC Details", "CLCDetail - clcDtlLst");
        sheetNameMapping.put("CLC Details", "CLCDetail - clcnonStandardIPLst");
        sheetNameMapping.put("Client Plan", "Client Plan - cltPlanIP");
        sheetNameMapping.put("Compound", "Compound - cmpndIPLst");
        sheetNameMapping.put("Compound Dosage Form", "CmpndDs - compoundDosageFormLst");
        sheetNameMapping.put("Compound - History Control", "Cmpnd - compoundHistoryCntrlLst");
        sheetNameMapping.put("Copay", "Copay - copayIPLst");
        sheetNameMapping.put("Copay Modifier", "CopayModif - copayModifierIPLst");
        sheetNameMapping.put("Cumulative Refill", "CumlativR - cumulativeRefillLst");
        sheetNameMapping.put("Custom DUR", "Custom DUR - customDurLst");
        sheetNameMapping.put("Custom Message", "Custom Message - customMsgLst");
        sheetNameMapping.put("Default DAW Options", "DefaultDAWOpt - dawPnltyIPLst");
        sheetNameMapping.put("DEA Class", "DEA Class - deaClassLst");
        sheetNameMapping.put("Accum Deductible", "Accum Ded - dedtblIPCardLst");
        sheetNameMapping.put("DESI Indicator", "DESI Indicator - desiDrugsLst");
        sheetNameMapping.put("Program", "Program - drugCvrgIPLst");
        sheetNameMapping.put("Formulary", "Formulary - frmlyIPLst");
        sheetNameMapping.put("Accum Benefit Max", "Accum Benefit Max - mabIPCrdLst");
        sheetNameMapping.put("Maintenance Edit", "Maint Edit - maintenanceEditLst");
        sheetNameMapping.put("Maintenance Program", "Maint Program - maintPgmIPLst");
        sheetNameMapping.put("Max Days Supply", "MxDySu - maximumDaysSupplyIPLst");
        sheetNameMapping.put("Member Specific Pat Pay", "MbrSpPatPy - mbrSpecificPPIPLst");
        sheetNameMapping.put("Additional Member Eligibility", "AddMe - memberEligibilityAddLst");
        sheetNameMapping.put("Member Eligibility COB Details", "MECOB - memberEligibilityCobLst");
        sheetNameMapping.put("Member Eligibility", "MbrElig - memberEligibilityLst");
        sheetNameMapping.put("Max Days Supply", "Max Days Supply - mxdyIPLst");
        sheetNameMapping.put("MaxDays Price Override", "MxDy PriceOvrd - mxDyPriceOrLst");
        sheetNameMapping.put("NPI", "NPI - npiLst");
        sheetNameMapping.put("Accum Out of Pocket & TROOP", "Accum OOP & TROOP - oopIPCrdLst");
        sheetNameMapping.put("Other Patient Pay", "Other PP - otherPtntPayIPLst");
        sheetNameMapping.put("PrescriberNetworkDetail", "PN- prescriberNetworkDetailLst");
        sheetNameMapping.put("PrescriberNetworkDetail", "Pre - prescriberPharmacistIPLst");
        sheetNameMapping.put("PSC Step Penalty", "PSC Step - pscStepPenaltyLst");
        sheetNameMapping.put("Default Patient Pay", "Default PP - ptntPayCrdLst");
        sheetNameMapping.put("Copay Modifier", "CpyMod - ptntPayModifierCrdLst");
        sheetNameMapping.put("Refill Limits", "Refill Limits - refilLmtIPLst");
        sheetNameMapping.put("Refill-Fill Limits", "Refil-FilLmts - rflFillLimitLst");
        sheetNameMapping.put("ROA", "ROA - roaLst");
        sheetNameMapping.put("Rx OTC", "Rx OTC - rxOtcLst");
        sheetNameMapping.put("SRX Profile", "SRX Profile - srxPrflIPLst");
        sheetNameMapping.put("TF Profile Attachment", "TFPrfileA - TfProfileAttachment");
        sheetNameMapping.put("Third Party Exceptions", "TPE - thirdPartyExcptionLst");
        sheetNameMapping.put("Third Party Exceptions", "TPE - tpeLst");
        sheetNameMapping.put("User Message", "User Message - userMessageLst");
        sheetNameMapping.put("XREF Profile Fast Pass", "XREFProfileFP - xrefFastPassLst");
        // ... add more mappings as needed

        // Iterate over sheets and rename them
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String oldName = workbook.getSheetName(i);
            String newName = sheetNameMapping.getOrDefault(oldName, oldName);
            workbook.setSheetName(i, newName);
        }
    }
}
