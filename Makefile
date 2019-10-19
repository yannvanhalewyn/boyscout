SRC_FILES  := $(shell find src -type f)
CSS_FILES  := $(shell find resources/css -type f)
TARGET_JS  := build/js/main.js
TARGET_CSS := build/css/application.css
TARGET_DEV_CSS := resources/public/css/application.css

default: build

repl:
	shadow-cljs watch app test

css-watch: $(TARGET_DEV_CSS)
	fsevent_watch -F $(CSS_FILES) | xargs -I{} make $(TARGET_DEV_CSS)

$(TARGET_JS): $(SRC_FILES)
	@echo "---- Building cljs"
	shadow-cljs release prod

$(TARGET_CSS): $(CSS_FILES)
	@echo "---- Building css"
	npx tailwind build $^ -o $@

$(TARGET_DEV_CSS): $(CSS_FILES)
	npx tailwind build $^ -o $@

index.html: resources/public/index.html
	cat $^ | sed 's|css/application.css|build/css/application.css|' | sed 's|js/app|build/js|' | sed 's|favicon.ico|./resources/public/favicon.ico|' > $@

cp-img:
	@echo "---- Copying resources"
	@[ -d build/img ] || mkdir build/img
	cp -r resources/public/img/* build/img

build: $(TARGET_JS) $(TARGET_CSS) index.html cp-img

stash:
	@git diff --quiet || git stash save "Stash before release"

release: stash build
	git checkout master
	@echo "Creating new gh-pages branch"
	git branch -D gh-pages || echo ''
	git checkout -b gh-pages
	@echo "Creating release commit"
	git add build index.html
	git commit -m "Release"
	@echo "Pushing release to GitHub"
	git push -f origin gh-pages
	git checkout master
