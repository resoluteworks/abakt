include gradle.properties

test:
	./gradlew clean test
	COVERALLS_REPO_TOKEN=$$COVERALLS_ABAKT ./gradlew coverallsJacoco

publish:
	./gradlew publishAggregationToCentralPortal

publish-local:
	./gradlew publish

release: test publish-local publish
	@echo $(abaktVersion)
	git tag "v$(abaktVersion)" -m "Release v$(abaktVersion)"
	git push --tags --force
	@echo Finished building version $(abaktVersion)
