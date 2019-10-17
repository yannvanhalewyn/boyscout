SRC_FILES  := $(shell find src -type f)
CSS_FILES  := $(shell find resources/scss -type f)
TARGET_JS  := build/js/main.js
TARGET_CSS := build/css/application.css
TARGET_DEV_CSS := resources/public/css/application.css

default: build

repl:
	shadow-cljs watch app test

css-watch: $(TARGET_DEV_CSS)
	fsevent_watch -F ./resources/scss | xargs -I{} make $(TARGET_DEV_CSS)

$(TARGET_JS): $(SRC_FILES)
	@echo "---- Building cljs"
	shadow-cljs release prod

$(TARGET_CSS): $(CSS_FILES)
	npx tailwind build $^ -o $@

$(TARGET_DEV_CSS): $(CSS_FILES)
	npx tailwind build $^ -o $@

index.html: resources/public/index.html
	cat $^ | sed 's|css/application.css|build/css/application.css|' | sed 's|js/app|build/js|' | sed 's|favicon.ico|./resources/public/favicon.ico|' > $@

build: $(TARGET_JS) $(TARGET_CSS) index.html
	@echo "---- Copying resources"
	cp -r resources/public/img build/img
