/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.model.Repository;
import com.liferay.portal.portletfilerepository.PortletFileRepositoryThreadLocal;
import com.liferay.portal.portletfilerepository.PortletFileRepositoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;

/**
 * @author Sergio González
 * @author Matthew Kong
 * @author Alexander Chow
 */
public class TempFileUtil {

	public static FileEntry addTempFile(
			long groupId, long userId, String fileName, String tempFolderName,
			File file, String mimeType)
		throws PortalException, SystemException {

		Folder folder = addTempFolder(groupId, userId, tempFolderName);

		boolean fileMaxSizeCheckEnabled =
			PortletFileRepositoryThreadLocal.isFileMaxSizeCheckEnabled();

		try {
			PortletFileRepositoryThreadLocal.setFileMaxSizeCheckEnabled(false);

			return PortletFileRepositoryUtil.addPortletFileEntry(
				groupId, userId, StringPool.BLANK, 0,
				PortletKeys.DOCUMENT_LIBRARY, folder.getFolderId(), file,
				fileName, mimeType, false);
		}
		finally {
			PortletFileRepositoryThreadLocal.setFileMaxSizeCheckEnabled(
				fileMaxSizeCheckEnabled);
		}
	}

	public static FileEntry addTempFile(
			long groupId, long userId, String fileName, String tempFolderName,
			InputStream inputStream, String mimeType)
		throws PortalException, SystemException {

		Folder folder = addTempFolder(groupId, userId, tempFolderName);

		boolean fileMaxSizeCheckEnabled =
			PortletFileRepositoryThreadLocal.isFileMaxSizeCheckEnabled();

		try {
			PortletFileRepositoryThreadLocal.setFileMaxSizeCheckEnabled(false);

			return PortletFileRepositoryUtil.addPortletFileEntry(
				groupId, userId, StringPool.BLANK, 0,
				PortletKeys.DOCUMENT_LIBRARY, folder.getFolderId(), inputStream,
				fileName, mimeType, false);
		}
		finally {
			PortletFileRepositoryThreadLocal.setFileMaxSizeCheckEnabled(
				fileMaxSizeCheckEnabled);
		}
	}

	public static void deleteTempFile(long fileEntryId)
		throws PortalException, SystemException {

		PortletFileRepositoryUtil.deletePortletFileEntry(fileEntryId);
	}

	public static void deleteTempFile(
			long groupId, long userId, String fileName, String tempFolderName)
		throws PortalException, SystemException {

		Folder folder = getTempFolder(groupId, userId, tempFolderName);

		PortletFileRepositoryUtil.deletePortletFileEntry(
			groupId, folder.getFolderId(), fileName);
	}

	public static FileEntry getTempFile(
			long groupId, long userId, String fileName, String tempFolderName)
		throws PortalException, SystemException {

		Folder folder = getTempFolder(groupId, userId, tempFolderName);

		return PortletFileRepositoryUtil.getPortletFileEntry(
			groupId, folder.getFolderId(), fileName);
	}

	public static String[] getTempFileEntryNames(
			long groupId, long userId, String tempFolderName)
		throws PortalException, SystemException {

		Folder folder = addTempFolder(groupId, userId, tempFolderName);

		List<FileEntry> fileEntries =
			PortletFileRepositoryUtil.getPortletFileEntries(
				groupId, folder.getFolderId());

		String[] fileEntryNames = new String[fileEntries.size()];

		for (int i = 0; i < fileEntries.size(); i++) {
			FileEntry fileEntry = fileEntries.get(i);
			fileEntryNames[i] = fileEntry.getTitle();
		}

		return fileEntryNames;
	}
	public static String[] getTempFileEntryNamesRecursive(
			long groupId, long userId, String tempFolderName)
					throws PortalException, SystemException {
		Folder folder = addTempFolder(groupId, userId, tempFolderName);
		List<String>files = new ArrayList<String>();
		String [] arrayFiles = getTempFileEntryNames(groupId, userId, tempFolderName);
		for(String file :arrayFiles){
			files.add(tempFolderName + StringPool.FORWARD_SLASH + file); 
		}
		List<Long> folderIds = DLAppServiceUtil.getSubfolderIds(folder.getRepositoryId(), folder.getFolderId(), true);
		for(Long folderId: folderIds){
			
			try{
				DLFolder dlf = DLFolderLocalServiceUtil.getDLFolder(folderId);
				String path = dlf.getPath();
				if(path.indexOf(tempFolderName) != -1){
					path = path.substring(path.indexOf(tempFolderName) + tempFolderName.length());
					System.out.println(path);
				}
				
				arrayFiles = getTempFileEntryNames(groupId, userId, tempFolderName +  path);
				for(String file :arrayFiles){
					files.add(tempFolderName + path + StringPool.FORWARD_SLASH + file); 
				}
			}catch(Exception ex){
				
			}
		}
		String[] fileEntryNames = {} ;
		fileEntryNames = files.toArray(fileEntryNames);
	
		return fileEntryNames;
	}

	protected static Folder addTempFolder(
			long groupId, long userId, String tempFolderName)
		throws PortalException, SystemException {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);

		Repository repository = PortletFileRepositoryUtil.addPortletRepository(
			groupId, PortletKeys.DOCUMENT_LIBRARY, serviceContext);

		Folder userFolder = PortletFileRepositoryUtil.addPortletFolder(
			userId, repository.getRepositoryId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, String.valueOf(userId),
			serviceContext);
		Folder tempFolder = null;
		Folder parent = userFolder;
		if(tempFolderName.indexOf(StringPool.FORWARD_SLASH) != -1){
			String [] patheElements = tempFolderName.split(StringPool.FORWARD_SLASH);
			for(String folderName:patheElements){
				if(Validator.isNull(folderName)){
					continue;
				}
				tempFolder = PortletFileRepositoryUtil.addPortletFolder(userId, repository.getRepositoryId(), parent.getFolderId(), folderName, serviceContext);
				parent = tempFolder;
			}
		}else{
			tempFolder = PortletFileRepositoryUtil.addPortletFolder(
				userId, repository.getRepositoryId(), userFolder.getFolderId(),
				tempFolderName, serviceContext);
		}

		return tempFolder;
	}

	protected static Folder getTempFolder(
			long groupId, long userId, String tempFolderName)
		throws PortalException, SystemException {

		Repository repository = PortletFileRepositoryUtil.getPortletRepository(
			groupId, PortletKeys.DOCUMENT_LIBRARY);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);

		Folder userFolder = PortletFileRepositoryUtil.getPortletFolder(
			userId, repository.getRepositoryId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, String.valueOf(userId),
			serviceContext);

		Folder tempFolder = null;
		Folder parent = userFolder;
		if(tempFolderName.indexOf(StringPool.FORWARD_SLASH) != -1){
			String [] pathElements = tempFolderName.split(StringPool.FORWARD_SLASH);
			for(String folderName:pathElements){
				if(Validator.isNull(folderName)) continue;
				tempFolder = PortletFileRepositoryUtil.getPortletFolder(userId,repository.getRepositoryId(), parent.getFolderId(), folderName,serviceContext);
				parent = tempFolder;
			}
		}else{
			tempFolder = PortletFileRepositoryUtil.getPortletFolder(
					userId, repository.getRepositoryId(), userFolder.getFolderId(),
					tempFolderName, serviceContext);
		}
		

		return tempFolder;
	}
	
	public static Folder getOrAddFolder(long userId,long repositoryId, long parentFolderId, String folderName, String description,ServiceContext serviceContext)
			throws PortalException, SystemException{
		Folder tempFolder;
		try {
			tempFolder = PortletFileRepositoryUtil.getPortletFolder(userId,repositoryId, parentFolderId, folderName,serviceContext);
		} catch (NoSuchFolderException ex) {
			tempFolder = PortletFileRepositoryUtil.addPortletFolder(userId, repositoryId, parentFolderId, folderName, serviceContext);
		}
		
		return tempFolder;
	}

}