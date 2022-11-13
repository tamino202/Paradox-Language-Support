package icu.windea.pls.core.psi;

import com.intellij.ide.scratch.*;
import com.intellij.injected.editor.*;
import com.intellij.model.*;
import com.intellij.openapi.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.*;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.search.*;
import com.intellij.testFramework.*;
import com.intellij.util.containers.*;
import com.intellij.util.indexing.*;
import icu.windea.pls.core.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static com.intellij.psi.impl.PsiManagerImpl.ANY_PSI_CHANGE_TOPIC;

//com.intellij.psi.impl.file.impl.ResolveScopeManagerImpl

/**
 * 默认情况下，无法从项目文件中的声明导航到库中的引用，除非对应的库已导出，
 * 这里覆盖了默认的实现逻辑，当必要时，得到的使用作用域也包含模块的依赖中的文件。
 * <p>
 * （见<code>Project Structure > Project Settings > Modules</code>）
 */
@SuppressWarnings("UnstableApiUsage")
public final class ParadoxResolveScopeManager extends ResolveScopeManager implements Disposable {
	private final Project myProject;
	private final ProjectRootManager myProjectRootManager;
	private final PsiManager myManager;

	private final Map<VirtualFile, GlobalSearchScope> myDefaultResolveScopesCache;
	private final AdditionalIndexableFileSet myAdditionalIndexableFileSet;

	public ParadoxResolveScopeManager(Project project) {
		myProject = project;
		myProjectRootManager = ProjectRootManager.getInstance(project);
		myManager = PsiManager.getInstance(project);
		myAdditionalIndexableFileSet = new AdditionalIndexableFileSet(project);

		myDefaultResolveScopesCache = ConcurrentFactoryMap.create(this::createScopeByFile, ContainerUtil::createConcurrentWeakKeySoftValueMap);

		myProject.getMessageBus().connect(this).subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
			@Override
			public void beforePsiChanged(boolean isPhysical) {
				if (isPhysical) myDefaultResolveScopesCache.clear();
			}
		});

		// Make it explicit that registering and removing ResolveScopeProviders needs to clear the resolve scope cache
		// (even though normally registerRunnableToRunOnChange would be enough to clear the cache)
		ResolveScopeProvider.EP_NAME.addChangeListener(() -> myDefaultResolveScopesCache.clear(), this);
		ResolveScopeEnlarger.EP_NAME.addChangeListener(() -> myDefaultResolveScopesCache.clear(), this);
	}

	@NotNull
	private GlobalSearchScope createScopeByFile(@NotNull VirtualFile key) {
		VirtualFile file = key;
		VirtualFile original = key instanceof LightVirtualFile ? ((LightVirtualFile)key).getOriginalFile() : null;
		if (original != null) {
			file = original;
		}
		GlobalSearchScope scope = null;
		for (ResolveScopeProvider resolveScopeProvider : ResolveScopeProvider.EP_NAME.getExtensionList()) {
			scope = resolveScopeProvider.getResolveScope(file, myProject);
			if (scope != null) break;
		}
		if (scope == null) scope = getInherentResolveScope(file);
		for (ResolveScopeEnlarger enlarger : ResolveScopeEnlarger.EP_NAME.getExtensions()) {
			SearchScope extra = enlarger.getAdditionalResolveScope(file, myProject);
			if (extra != null) {
				scope = scope.union(extra);
			}
		}
		if (original != null && !scope.contains(key)) {
			scope = scope.union(GlobalSearchScope.fileScope(myProject, key));
		}
		return scope;
	}

	@NotNull
	private GlobalSearchScope getResolveScopeFromProviders(@NotNull VirtualFile vFile) {
		return myDefaultResolveScopesCache.get(vFile);
	}

	@NotNull
	private GlobalSearchScope getInherentResolveScope(@NotNull VirtualFile vFile) {
		ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
		com.intellij.openapi.module.Module module = projectFileIndex.getModuleForFile(vFile);
		if (module != null) {
			boolean includeTests = TestSourcesFilter.isTestSources(vFile, myProject);
			return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
		}

		if (!projectFileIndex.isInLibrary(vFile)) {
			GlobalSearchScope allScope = GlobalSearchScope.allScope(myProject);
			if (!allScope.contains(vFile)) {
				return GlobalSearchScope.fileScope(myProject, vFile).uniteWith(allScope);
			}
			return allScope;
		}

		return LibraryScopeCache.getInstance(myProject).getLibraryScope(projectFileIndex.getOrderEntriesForFile(vFile));
	}

	@Override
	@NotNull
	public GlobalSearchScope getResolveScope(@NotNull PsiElement element) {
		ProgressIndicatorProvider.checkCanceled();

		if (element instanceof PsiDirectory) {
			return getResolveScopeFromProviders(((PsiDirectory)element).getVirtualFile());
		}

		PsiFile containingFile = element.getContainingFile();
		if (containingFile instanceof PsiCodeFragment) {
			GlobalSearchScope forcedScope = ((PsiCodeFragment)containingFile).getForcedResolveScope();
			if (forcedScope != null) {
				return forcedScope;
			}
		}

		if (containingFile != null) {
			PsiElement context = containingFile.getContext();
			if (context != null) {
				return withFile(containingFile, getResolveScope(context));
			}
		}

		if (containingFile == null) {
			return GlobalSearchScope.allScope(myProject);
		}
		GlobalSearchScope scope = getPsiFileResolveScope(containingFile);
		ModelBranch branch = ModelBranch.getPsiBranch(containingFile);
		return branch != null ? ((ModelBranchImpl)branch).modifyScope(scope) : scope;
	}

	@NotNull
	private GlobalSearchScope getPsiFileResolveScope(@NotNull PsiFile psiFile) {
		if (psiFile instanceof FileResolveScopeProvider) {
			return ((FileResolveScopeProvider)psiFile).getFileResolveScope();
		}
		if (!psiFile.getOriginalFile().isPhysical() && !psiFile.getViewProvider().isPhysical()) {
			return withFile(psiFile, GlobalSearchScope.allScope(myProject));
		}
		return getResolveScopeFromProviders(psiFile.getViewProvider().getVirtualFile());
	}

	private GlobalSearchScope withFile(PsiFile containingFile, GlobalSearchScope scope) {
		return PsiSearchScopeUtil.isInScope(scope, containingFile)
			? scope
			: scope.uniteWith(GlobalSearchScope.fileScope(myProject, containingFile.getViewProvider().getVirtualFile()));
	}


	@NotNull
	@Override
	public GlobalSearchScope getDefaultResolveScope(@NotNull VirtualFile vFile) {
		PsiFile psiFile = myManager.findFile(vFile);
		assert psiFile != null : "directory=" + vFile.isDirectory() + "; " + myProject+"; vFile="+vFile+"; type="+vFile.getFileType();
		return getResolveScopeFromProviders(vFile);
	}


	@Override
	@NotNull
	public GlobalSearchScope getUseScope(@NotNull PsiElement element) {
		VirtualFile vDirectory;
		VirtualFile virtualFile;
		PsiFile containingFile;
		GlobalSearchScope allScope = GlobalSearchScope.allScope(myManager.getProject());
		if (element instanceof PsiDirectory) {
			vDirectory = ((PsiDirectory)element).getVirtualFile();
			virtualFile = null;
			containingFile = null;
		}
		else {
			containingFile = element.getContainingFile();
			if (containingFile == null) return allScope;
			virtualFile = containingFile.getVirtualFile();
			if (virtualFile == null) return allScope;
			if (virtualFile instanceof VirtualFileWindow) {
				return GlobalSearchScope.fileScope(myProject, ((VirtualFileWindow)virtualFile).getDelegate());
			}
			if (ScratchUtil.isScratch(virtualFile)) {
				return GlobalSearchScope.fileScope(myProject, virtualFile);
			}
			vDirectory = virtualFile.getParent();
		}

		if (vDirectory == null) return allScope;
		ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
		VirtualFile notNullVFile = virtualFile != null ? virtualFile : vDirectory;
		Module module = projectFileIndex.getModuleForFile(notNullVFile);
		if(useAllScope(element)) {
			return allScope;
		}
		if (module == null) {
			List<OrderEntry> entries = projectFileIndex.getOrderEntriesForFile(notNullVFile);
			if (entries.isEmpty() && (myAdditionalIndexableFileSet.isInSet(notNullVFile) || isFromAdditionalLibraries(notNullVFile))) {
				return allScope;
			}
			
			GlobalSearchScope result = LibraryScopeCache.getInstance(myProject).getLibraryUseScope(entries);
			return containingFile == null || virtualFile.isDirectory() || result.contains(virtualFile)
				? result : GlobalSearchScope.fileScope(containingFile).uniteWith(result);
		}
		boolean isTest = TestSourcesFilter.isTestSources(vDirectory, myProject);
		return isTest
			? GlobalSearchScope.moduleTestsWithDependentsScope(module)
			: GlobalSearchScope.moduleWithDependentsScope(module);
	}

	private static boolean useAllScope(PsiElement element) {
		return ExtensionsKt.useAllUseScope(element);
	}

	private boolean isFromAdditionalLibraries(@NotNull VirtualFile file) {
		for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
			for (SyntheticLibrary library : provider.getAdditionalProjectLibraries(myProject)) {
				if (library.contains(file)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void dispose() {

	}
}