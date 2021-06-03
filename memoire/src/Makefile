MAIN=main

pdf: clean $(MAIN).tex preambule.tex introduction.tex chapitre1.tex chapitre2.tex chapitre3.tex conclusion.tex
	latexmk -pdf -f -interaction=nonstopmode $(MAIN).tex

clean:
	latexmk -c
