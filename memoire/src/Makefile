MAIN=main

pdf: clean $(MAIN).tex
	latexmk -pdf -f -interaction=nonstopmode $(MAIN).tex

clean:
	latexmk -c
