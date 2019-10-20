BUILD_DIR  := build
IMG_DIR    := resources/public/img

SRC_FILES  := $(shell find src -type f)
CSS_FILES  := $(shell find resources/css -type f)
IMG_FILES  := $(shell find $(IMG_DIR) -type f)
TARGET_JS  := $(BUILD_DIR)/js/main.js
TARGET_CSS := $(BUILD_DIR)/css/application.css
TARGET_CSS_DEV := resources/public/css/application.css
TARGET_IMG_FILES := $(patsubst $(IMG_DIR)/%, $(BUILD_DIR)/img/%, $(IMG_FILES))

default: build

repl:
	shadow-cljs watch app test

css-watch: $(TARGET_DEV_CSS)
	fsevent_watch -F $(CSS_FILES) | xargs -I{} make $(TARGET_CSS_DEV)

$(TARGET_JS): $(SRC_FILES)
	@echo "---- Building cljs"
	shadow-cljs release prod

$(TARGET_CSS): $(CSS_FILES) $(SRC_FILES)
	@echo "---- Building css"
	npx postcss $< -o $@

$(TARGET_CSS_DEV): $(CSS_FILES)
	npx tailwind build $^ -o $@

$(TARGET_IMG_FILES): $(BUILD_DIR)/img/%: $(IMG_DIR)/%
	@[ -d build/img ] || mkdir build/img
	cp $< $@

index.html: resources/public/index.html
	cat $^ | sed 's|css/application.css|$(BUILD_DIR)/css/application.css|' | sed 's|js/app|$(BUILD_DIR)/js|' | sed 's|favicon.ico|./resources/public/favicon.ico|' > $@

build: $(TARGET_JS) $(TARGET_CSS) $(TARGET_IMG_FILES) index.html

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
